package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.service.subscription.StripeCreditPurchaseService.CREDIT_PURCHASE_ID_METADATA_KEY;
import static app.bpartners.api.service.subscription.StripeSetupService.isPaymentMethodReplacement;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentFailureNotificationRequested;
import app.bpartners.api.endpoint.event.model.UserDefaultPaymentMethodBackfillRequested;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.credit.CreditPurchaseService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {
  private static final String INVOICE_PAID = "invoice.paid";
  private static final String INVOICE_PAYMENT_FAILED = "invoice.payment_failed";
  private static final String STRIPE_SUBSCRIPTION_CYCLE_BILLING_REASON = "subscription_cycle";
  private static final String CHARGE_REFUNDED = "charge.refunded";
  private static final String PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
  private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
  private static final String STRIPE_CHECKOUT_PAID_STATUS = "paid";
  private static final String STRIPE_CHECKOUT_SETUP_MODE = "setup";

  private final StripeConf stripeConf;
  private final UserRepository userRepository;
  private final EventProducer eventProducer;
  private final SubscriptionService subscriptionService;
  private final CreditPurchaseService creditPurchaseService;
  private final StripePaymentMethodService stripePaymentMethodService;
  private final SubscriptionPaymentService subscriptionPaymentService;
  private final UserSubscriptionProductService userSubscriptionProductService;
  private final CreditGrantService creditGrantService;

  public void handleEvent(String payload, String signatureHeader) {
    var event = verifySignature(payload, signatureHeader);
    if (INVOICE_PAID.equals(event.getType())) {
      handleInvoicePaid(event);
      return;
    }
    if (INVOICE_PAYMENT_FAILED.equals(event.getType())) {
      handleInvoicePaymentFailed(event);
      return;
    }
    if (CHARGE_REFUNDED.equals(event.getType())) {
      handleChargeRefunded(event);
      return;
    }
    if (PAYMENT_INTENT_SUCCEEDED.equals(event.getType())) {
      handlePaymentIntentSucceeded(event);
      return;
    }
    if (CHECKOUT_SESSION_COMPLETED.equals(event.getType())) {
      handleCheckoutSessionCompleted(event);
      return;
    }
    log.info("Ignoring unhandled Stripe event type={}", event.getType());
  }

  private void handleInvoicePaid(Event event) {
    var invoice = extractStripeObject(event, Invoice.class);
    if (invoice == null) {
      return;
    }
    requestDefaultPaymentMethodBackfill(invoice.getCustomer(), INVOICE_PAID, invoice.getId());
    var invoiceSubscriptionIdentifier = invoice.getSubscription();
    subscriptionService.cancelScheduledSubscriptionAfterInvoicePaid(invoiceSubscriptionIdentifier);
    if (isEssentialSubscriptionInvoice(invoice)) {
      subscriptionService.cancelSubscriptionImmediately(invoiceSubscriptionIdentifier);
      return;
    }
    subscriptionPaymentService
        .recordPaidStripeInvoice(invoice)
        .ifPresent(this::grantIncludedCreditsForPaidSubscription);
  }

  private void handleInvoicePaymentFailed(Event event) {
    var invoice = extractStripeObject(event, Invoice.class);
    if (invoice == null) {
      return;
    }
    if (invoice.getSubscription() == null) {
      log.info(
          "Stripe Invoice(id={}) is not attached to a subscription, its payment failure is not"
              + " notified",
          invoice.getId());
      return;
    }
    if (!STRIPE_SUBSCRIPTION_CYCLE_BILLING_REASON.equals(invoice.getBillingReason())) {
      log.info(
          "Stripe Invoice(id={}) is not a subscription renewal (billingReason={}), its payment"
              + " failure is not notified",
          invoice.getId(),
          invoice.getBillingReason());
      return;
    }
    if (isPaymentRetry(invoice)) {
      log.info(
          "Stripe Invoice(id={}) payment failure is a retry (attemptCount={}), only the first"
              + " failed attempt is notified",
          invoice.getId(),
          invoice.getAttemptCount());
      return;
    }
    var optionalUser = userRepository.findByStripeCustomerId(invoice.getCustomer());
    if (optionalUser.isEmpty()) {
      log.warn(
          "No user found for Stripe customer id={}, payment failure of Stripe Invoice(id={}) is not"
              + " notified",
          invoice.getCustomer(),
          invoice.getId());
      return;
    }
    var userId = optionalUser.get().getId();
    var resolvedPlan = subscriptionPaymentService.resolvePlanFromStripe(invoice);
    var activeSubscription =
        userSubscriptionProductService.findActiveUserSubscriptionProduct(userId).orElse(null);
    var billingInterval = billingIntervalOf(resolvedPlan, activeSubscription);
    if (billingInterval != MONTHLY) {
      log.info(
          "Stripe Invoice(id={}) bills a {} subscription of User(id={}), only monthly subscription"
              + " payment failures are notified",
          invoice.getId(),
          billingInterval,
          userId);
      return;
    }
    var billedPeriod = subscriptionPaymentService.billedPeriodOf(invoice);
    eventProducer.accept(
        List.of(
            SubscriptionPaymentFailureNotificationRequested.builder()
                .userId(userId)
                .stripeInvoiceId(invoice.getId())
                .planName(planNameOf(resolvedPlan, activeSubscription))
                .amountInCentsWithVat(amountDueInCentsOf(invoice))
                .periodStartDatetime(epochSecondOrNull(billedPeriod.start()))
                .periodEndDatetime(epochSecondOrNull(billedPeriod.end()))
                .nextPaymentAttemptDatetime(epochSecondOrNull(invoice.getNextPaymentAttempt()))
                .paymentUrl(invoice.getHostedInvoiceUrl())
                .build()));
    log.info(
        "Requested subscription payment failure notification of User(id={}) from Stripe"
            + " Invoice(id={})",
        userId,
        invoice.getId());
  }

  private BillingInterval billingIntervalOf(
      ResolvedPlan resolvedPlan, UserSubscriptionProduct activeSubscription) {
    if (resolvedPlan != null && resolvedPlan.billingInterval() != null) {
      return resolvedPlan.billingInterval();
    }
    return activeSubscription == null ? null : activeSubscription.getBillingInterval();
  }

  private String planNameOf(ResolvedPlan resolvedPlan, UserSubscriptionProduct activeSubscription) {
    if (resolvedPlan != null && resolvedPlan.product() != null) {
      return resolvedPlan.product().getName();
    }
    if (activeSubscription == null || activeSubscription.getSubscriptionProduct() == null) {
      return null;
    }
    return activeSubscription.getSubscriptionProduct().getName();
  }

  private static boolean isPaymentRetry(Invoice invoice) {
    return invoice.getAttemptCount() != null && invoice.getAttemptCount() > 1L;
  }

  private static Long amountDueInCentsOf(Invoice invoice) {
    if (invoice.getAmountRemaining() != null && invoice.getAmountRemaining() > 0L) {
      return invoice.getAmountRemaining();
    }
    return invoice.getAmountDue() == null ? invoice.getTotal() : invoice.getAmountDue();
  }

  private static Instant epochSecondOrNull(Long epochSecond) {
    return epochSecond == null ? null : Instant.ofEpochSecond(epochSecond);
  }

  private void handleChargeRefunded(Event event) {
    var charge = extractStripeObject(event, Charge.class);
    if (charge == null) {
      return;
    }
    var stripeInvoiceId = charge.getInvoice();
    if (stripeInvoiceId == null) {
      log.info(
          "Stripe Charge(id={}) is not attached to an invoice, no subscription payment to refund",
          charge.getId());
      return;
    }
    if (!Boolean.TRUE.equals(charge.getRefunded())) {
      log.info(
          "Stripe Charge(id={}) is only partially refunded (amountRefunded={} of {}), keeping"
              + " SubscriptionPayment of Invoice(id={}) as billed",
          charge.getId(),
          charge.getAmountRefunded(),
          charge.getAmount(),
          stripeInvoiceId);
      return;
    }
    subscriptionPaymentService.markRefunded(stripeInvoiceId, refundedAtOf(event));
  }

  private Instant refundedAtOf(Event event) {
    return event.getCreated() == null ? null : Instant.ofEpochSecond(event.getCreated());
  }

  private void requestDefaultPaymentMethodBackfill(
      String stripeCustomerIdentifier, String stripeEventType, String stripeObjectId) {
    if (stripeCustomerIdentifier == null) {
      log.info(
          "Stripe event={} object(id={}) carries no customer, skipping default payment method"
              + " backfill",
          stripeEventType,
          stripeObjectId);
      return;
    }
    var optionalUser = userRepository.findByStripeCustomerId(stripeCustomerIdentifier);
    if (optionalUser.isEmpty()) {
      log.warn(
          "No user found for Stripe customer id={}, skipping default payment method backfill",
          stripeCustomerIdentifier);
      return;
    }
    var userId = optionalUser.get().getId();
    eventProducer.accept(
        List.of(UserDefaultPaymentMethodBackfillRequested.builder().userId(userId).build()));
    log.info(
        "Requested default payment method backfill for User(id={}) from Stripe event={}"
            + " object(id={})",
        userId,
        stripeEventType,
        stripeObjectId);
  }

  private boolean isEssentialSubscriptionInvoice(Invoice invoice) {
    var essentialSubscriptionProductId = stripeConf.getEssentialSubscriptionProductId();
    if (essentialSubscriptionProductId == null) {
      return false;
    }
    var billedStripeProductIds = stripeProductIdsOf(invoice);
    if (billedStripeProductIds.contains(essentialSubscriptionProductId)) {
      return true;
    }
    log.info(
        "Stripe Invoice(id={}) does not bill the essential subscription product (billed"
            + " products={}, essential product={}), skipping immediate cancellation",
        invoice.getId(),
        billedStripeProductIds,
        essentialSubscriptionProductId);
    return false;
  }

  private static List<String> stripeProductIdsOf(Invoice invoice) {
    var lines = invoice.getLines() == null ? null : invoice.getLines().getData();
    if (lines == null) {
      return List.of();
    }
    return lines.stream()
        .map(StripeWebhookService::stripeProductIdOf)
        .filter(Objects::nonNull)
        .toList();
  }

  private static String stripeProductIdOf(InvoiceLineItem line) {
    if (line.getPlan() != null && line.getPlan().getProduct() != null) {
      return line.getPlan().getProduct();
    }
    return line.getPrice() == null ? null : line.getPrice().getProduct();
  }

  private void grantIncludedCreditsForPaidSubscription(SubscriptionPayment payment) {
    var plan = payment.getSubscriptionProduct();
    if (plan == null) {
      log.info(
          "SubscriptionPayment(id={}) has no resolved plan, no subscription credits to grant",
          payment.getId());
      return;
    }
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        payment.getUserId(), plan.getId(), payment.getBillingInterval());
    if (payment.getBillingInterval() == MONTHLY && payment.getPeriodStartDatetime() != null) {
      creditGrantService.grantIncludedCreditsOfBilledMonth(
          payment.getUserId(), plan, payment.getPeriodStartDatetime());
      return;
    }
    creditGrantService.grantIncludedCredits(payment.getUserId(), plan);
  }

  private void handlePaymentIntentSucceeded(Event event) {
    var paymentIntent = extractStripeObject(event, PaymentIntent.class);
    if (paymentIntent == null) {
      return;
    }
    requestDefaultPaymentMethodBackfill(
        paymentIntent.getCustomer(), event.getType(), paymentIntent.getId());
    completeCreditPurchase(paymentIntent.getMetadata(), event.getType());
  }

  private void handleCheckoutSessionCompleted(Event event) {
    var session = extractStripeObject(event, Session.class);
    if (session == null) {
      return;
    }
    if (STRIPE_CHECKOUT_SETUP_MODE.equals(session.getMode())) {
      replacePaymentMethod(session);
      return;
    }
    if (!STRIPE_CHECKOUT_PAID_STATUS.equals(session.getPaymentStatus())) {
      log.info(
          "Stripe checkout session={} is not paid (paymentStatus={}), skipping",
          session.getId(),
          session.getPaymentStatus());
      return;
    }
    requestDefaultPaymentMethodBackfill(session.getCustomer(), event.getType(), session.getId());
    completeCreditPurchase(session.getMetadata(), event.getType());
  }

  private void replacePaymentMethod(Session session) {
    if (!isPaymentMethodReplacement(session.getMetadata())) {
      log.info(
          "Stripe setup session={} is not a payment method replacement, skipping", session.getId());
      return;
    }
    var stripeCustomerId = session.getCustomer();
    var setupIntentId = session.getSetupIntent();
    if (stripeCustomerId == null || setupIntentId == null) {
      log.warn(
          "Stripe setup session={} carries no customer (={}) or no setup intent (={}), unable to"
              + " replace payment method",
          session.getId(),
          stripeCustomerId,
          setupIntentId);
      return;
    }
    stripePaymentMethodService.replaceCardPaymentMethodsFromSetupIntent(
        stripeCustomerId, setupIntentId);
    log.info(
        "Replaced payment methods of StripeCustomer.id={} from Stripe setup session={}",
        stripeCustomerId,
        session.getId());
  }

  private void completeCreditPurchase(Map<String, String> metadata, String eventType) {
    var creditPurchaseId = metadata == null ? null : metadata.get(CREDIT_PURCHASE_ID_METADATA_KEY);
    if (creditPurchaseId == null) {
      log.info("Stripe event={} carries no credit purchase metadata, skipping", eventType);
      return;
    }
    creditPurchaseService
        .complete(creditPurchaseId)
        .ifPresent(
            completed ->
                log.info(
                    "CreditPurchase.id={} completed from Stripe event={}, CreditTransaction.id={}",
                    completed.getId(),
                    eventType,
                    completed.getCreditTransactionId()));
  }

  private Event verifySignature(String payload, String signatureHeader) {
    var webhookSecret = stripeConf.getWebhookSecret();
    if (webhookSecret == null || webhookSecret.isBlank()) {
      throw new BadRequestException("Stripe webhook secret is not configured");
    }
    try {
      return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
    } catch (SignatureVerificationException e) {
      throw new BadRequestException("Invalid Stripe webhook signature");
    }
  }

  private <T extends StripeObject> T extractStripeObject(Event event, Class<T> type) {
    var deserializer = event.getDataObjectDeserializer();
    StripeObject stripeObject = deserializer.getObject().orElse(null);
    if (stripeObject == null) {
      try {
        stripeObject = deserializer.deserializeUnsafe();
      } catch (Exception e) {
        log.error("Unable to deserialize Stripe event={} data object", event.getType(), e);
        return null;
      }
    }
    if (type.isInstance(stripeObject)) {
      return type.cast(stripeObject);
    }
    log.error("Stripe event={} data object is not a {}", event.getType(), type.getSimpleName());
    return null;
  }
}
