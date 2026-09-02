package app.bpartners.api.service.subscription;

import static app.bpartners.api.service.subscription.StripeCreditPurchaseService.CREDIT_PURCHASE_ID_METADATA_KEY;
import static app.bpartners.api.service.subscription.StripeSetupService.isPaymentMethodReplacement;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.credit.CreditPurchaseService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {
  private static final String SUBSCRIPTION_CREATED = "customer.subscription.created";
  private static final String SUBSCRIPTION_UPDATED = "customer.subscription.updated";
  private static final String SUBSCRIPTION_SCHEDULE_CREATED = "subscription_schedule.created";
  private static final String INVOICE_PAID = "invoice.paid";
  private static final String PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
  private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
  private static final String STRIPE_CHECKOUT_PAID_STATUS = "paid";
  private static final String STRIPE_CHECKOUT_SETUP_MODE = "setup";
  private static final String STRIPE_ACTIVE_STATUS = "active";
  private static final String STRIPE_SCHEDULE_NOT_STARTED_STATUS = "not_started";

  private final StripeConf stripeConf;
  private final UserRepository userRepository;
  private final EventProducer eventProducer;
  private final SubscriptionService subscriptionService;
  private final CreditPurchaseService creditPurchaseService;
  private final StripePaymentMethodService stripePaymentMethodService;
  private final SubscriptionPaymentService subscriptionPaymentService;

  public void handleEvent(String payload, String signatureHeader) {
    var event = verifySignature(payload, signatureHeader);
    if (INVOICE_PAID.equals(event.getType())) {
      handleInvoicePaid(event);
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
    var eligible = extractEligibleSubscription(event);
    if (eligible == null || eligible.customerId() == null) {
      return;
    }
    var optionalUser = userRepository.findByStripeCustomerId(eligible.customerId());
    if (optionalUser.isEmpty()) {
      log.warn("No user found for Stripe customer id={}, skipping", eligible.customerId());
      return;
    }
    var userId = optionalUser.get().getId();
    eventProducer.accept(
        List.of(
            UserSubscriptionProductBackfillRequested.builder()
                .userId(userId)
                .subscriptionProductId(eligible.subscriptionPlanIdentifier())
                .billingInterval(eligible.billingInterval())
                .subscriptionStartDatetime(eligible.subscriptionStartDatetime())
                .build()));
    log.info(
        "Requested UserSubscriptionProduct creation for User(id={}) from Stripe event={}",
        userId,
        event.getType());
  }

  private void handleInvoicePaid(Event event) {
    var invoice = extractStripeObject(event, Invoice.class);
    if (invoice == null) {
      return;
    }
    subscriptionService.cancelScheduledSubscriptionAfterInvoicePaid(invoice.getSubscription());
    subscriptionPaymentService.recordPaidStripeInvoice(invoice);
  }

  private void handlePaymentIntentSucceeded(Event event) {
    var paymentIntent = extractStripeObject(event, PaymentIntent.class);
    if (paymentIntent == null) {
      return;
    }
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

  private EligibleSubscription extractEligibleSubscription(Event event) {
    var type = event.getType();

    if (SUBSCRIPTION_CREATED.equals(type) || SUBSCRIPTION_UPDATED.equals(type)) {
      var subscription = extractStripeObject(event, Subscription.class);
      if (subscription == null || !isEligibleSubscription(subscription)) {
        log.info(
            "Stripe event={} subscription not active or cancelled at period end (status={},"
                + " cancelAtPeriodEnd={}), skipping",
            type,
            subscription == null ? null : subscription.getStatus(),
            subscription == null ? null : subscription.getCancelAtPeriodEnd());
        return null;
      }
      var subscribedPlan = subscriptionService.resolveSubscribedPlan(subscription).orElse(null);
      return eligibleSubscriptionOf(
          subscription.getCustomer(), subscribedPlan, startDatetimeOf(subscription));
    }

    if (SUBSCRIPTION_SCHEDULE_CREATED.equals(type)) {
      var schedule = extractStripeObject(event, SubscriptionSchedule.class);
      if (schedule == null || !isEligibleSchedule(schedule)) {
        log.info(
            "Stripe event={} subscription schedule not eligible (status={}), skipping",
            type,
            schedule == null ? null : schedule.getStatus());
        return null;
      }
      var subscribedPlan = subscriptionService.resolveSubscribedPlan(schedule).orElse(null);
      return eligibleSubscriptionOf(
          schedule.getCustomer(), subscribedPlan, startDatetimeOf(schedule));
    }
    log.info("Ignoring unhandled Stripe event type={}", type);
    return null;
  }

  private boolean isEligibleSubscription(Subscription subscription) {
    return STRIPE_ACTIVE_STATUS.equals(subscription.getStatus())
        && !Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd());
  }

  private boolean isEligibleSchedule(SubscriptionSchedule schedule) {
    return schedule.getCanceledAt() == null
        && (STRIPE_SCHEDULE_NOT_STARTED_STATUS.equals(schedule.getStatus())
            || STRIPE_ACTIVE_STATUS.equals(schedule.getStatus()));
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

  private static EligibleSubscription eligibleSubscriptionOf(
      String customerId,
      SubscriptionService.SubscribedPlan subscribedPlan,
      Instant subscriptionStartDatetime) {
    return subscribedPlan == null
        ? new EligibleSubscription(customerId, null, null, subscriptionStartDatetime)
        : new EligibleSubscription(
            customerId,
            subscribedPlan.planId(),
            subscribedPlan.billingInterval(),
            subscriptionStartDatetime);
  }

  private static Instant startDatetimeOf(Subscription subscription) {
    var currentPeriodStart = subscription.getCurrentPeriodStart();
    return currentPeriodStart == null ? null : Instant.ofEpochSecond(currentPeriodStart);
  }

  private static Instant startDatetimeOf(SubscriptionSchedule schedule) {
    var phases = schedule.getPhases();
    if (phases == null || phases.isEmpty()) {
      return null;
    }
    var startDate = phases.getFirst().getStartDate();
    return startDate == null ? null : Instant.ofEpochSecond(startDate);
  }

  private record EligibleSubscription(
      String customerId,
      String subscriptionPlanIdentifier,
      BillingInterval billingInterval,
      Instant subscriptionStartDatetime) {}
}
