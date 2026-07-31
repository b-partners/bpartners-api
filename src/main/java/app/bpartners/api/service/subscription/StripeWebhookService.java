package app.bpartners.api.service.subscription;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
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
  private static final String STRIPE_ACTIVE_STATUS = "active";

  private final StripeConf stripeConf;
  private final UserRepository userRepository;
  private final EventProducer eventProducer;

  public void handleEvent(String payload, String signatureHeader) {
    var event = verifySignature(payload, signatureHeader);
    if (!SUBSCRIPTION_CREATED.equals(event.getType())
        && !SUBSCRIPTION_UPDATED.equals(event.getType())) {
      log.info("Ignoring unhandled Stripe event type={}", event.getType());
      return;
    }
    var subscription = extractSubscription(event);
    if (subscription == null || !STRIPE_ACTIVE_STATUS.equals(subscription.getStatus())) {
      log.info(
          "Stripe event={} subscription not active (status={}), skipping",
          event.getType(),
          subscription == null ? null : subscription.getStatus());
      return;
    }
    var stripeCustomerId = subscription.getCustomer();
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

  private Subscription extractSubscription(Event event) {
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
    if (stripeObject instanceof Subscription subscription) {
      return subscription;
    }
    log.error("Stripe event={} data object is not a Subscription", event.getType());
    return null;
  }
}
