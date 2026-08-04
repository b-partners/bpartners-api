package app.bpartners.api.service.subscription;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.net.Webhook;
import java.util.List;
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
  private static final String STRIPE_ACTIVE_STATUS = "active";
  private static final String STRIPE_SCHEDULE_NOT_STARTED_STATUS = "not_started";

  private final StripeConf stripeConf;
  private final UserRepository userRepository;
  private final EventProducer eventProducer;
  private final SubscriptionService subscriptionService;

  public void handleEvent(String payload, String signatureHeader) {
    var event = verifySignature(payload, signatureHeader);
    if (INVOICE_PAID.equals(event.getType())) {
      handleInvoicePaid(event);
      return;
    }
    var stripeCustomerId = extractEligibleCustomerId(event);
    if (stripeCustomerId == null) {
      return;
    }
    var optionalUser = userRepository.findByStripeCustomerId(stripeCustomerId);
    if (optionalUser.isEmpty()) {
      log.warn("No user found for Stripe customer id={}, skipping", stripeCustomerId);
      return;
    }
    var userId = optionalUser.get().getId();
    eventProducer.accept(
        List.of(UserSubscriptionProductBackfillRequested.builder().userId(userId).build()));
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
  }

  private String extractEligibleCustomerId(Event event) {
    var type = event.getType();
    if (SUBSCRIPTION_CREATED.equals(type) || SUBSCRIPTION_UPDATED.equals(type)) {
      var subscription = extractStripeObject(event, Subscription.class);
      if (subscription == null || !STRIPE_ACTIVE_STATUS.equals(subscription.getStatus())) {
        log.info(
            "Stripe event={} subscription not active (status={}), skipping",
            type,
            subscription == null ? null : subscription.getStatus());
        return null;
      }
      return subscription.getCustomer();
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
      return schedule.getCustomer();
    }
    log.info("Ignoring unhandled Stripe event type={}", type);
    return null;
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
}
