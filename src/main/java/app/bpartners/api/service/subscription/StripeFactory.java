package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static app.bpartners.api.payment.StripeConf.defaultCurrency;
import static com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION;
import static com.stripe.param.checkout.SessionCreateParams.PaymentMethodCollection.IF_REQUIRED;
import static com.stripe.param.checkout.SessionCreateParams.UiMode.HOSTED;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionSession;
import app.bpartners.api.repository.jpa.UserSubscriptionSessionRepository;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.SubscriptionScheduleCreateParams;
import com.stripe.param.SubscriptionScheduleListParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeFactory {
  public static final String OVERAGE_METERED_PRICE_ID_METADATA_KEY = "overage_metered_price_id";
  public static final String OVERAGE_BILLING_CYCLE_ANCHOR_METADATA_KEY =
      "overage_billing_cycle_anchor";
  private static final long ANNUAL_OVERAGE_MONTHLY_ITERATIONS = 12L;
  private final TemporalUtils temporalUtils;
  private final UserSubscriptionSessionRepository userSubscriptionSessionRepository;

  public List<com.stripe.model.Subscription> retrieveUserSubscriptions(User user)
      throws StripeException {
    SubscriptionListParams params =
        SubscriptionListParams.builder().setCustomer(user.getUserSubscriptionId()).build();
    return com.stripe.model.Subscription.list(params).getData();
  }

  public Redirection initiateSubscriptionWorkflow(
      Customer stripeCustomer,
      Price price,
      RedirectionStatusUrls redirectionUrls,
      long billingCycleAnchor,
      Subscription subscription)
      throws StripeException {
    var sessionSubscription =
        createSessionSubscription(
            stripeCustomer, subscription, price, redirectionUrls, billingCycleAnchor);
    return mapFromSession(sessionSubscription, redirectionUrls);
  }

  private Redirection mapFromSession(Session session, RedirectionStatusUrls redirectionUrls) {
    return new Redirection()
        .redirectionUrl(session.getUrl())
        .redirectionStatusUrls(redirectionUrls);
  }

  public Session createSessionSubscription(
      Customer stripeCustomer,
      Subscription subscription,
      Price newVariableProductPrice,
      RedirectionStatusUrls redirectionUrls,
      Long billingCycleAnchor)
      throws StripeException {
    var subscriptionProduct = subscription.getSubscriptionProduct();
    var basePlanLineItem =
        subscription.getBillingInterval() == YEARLY
            ? SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPrice(subscriptionProduct.getAnnualE2PriceId())
                .build()
            : SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(
                    SessionCreateParams.LineItem.PriceData.builder()
                        .setProduct(subscriptionProduct.getE2Id())
                        .setCurrency(defaultCurrency())
                        .setUnitAmount(subscriptionProduct.getPriceInCentsWithVat())
                        .setRecurring(
                            computeRecurringFromSubscriptionProductForSubscriptionMode(
                                subscriptionProduct))
                        .build())
                .build();
    var sessionParamsBuilder =
        SessionCreateParams.builder()
            .setMode(SUBSCRIPTION)
            .setCustomer(stripeCustomer.getId())
            .setCurrency(defaultCurrency())
            .addLineItem(basePlanLineItem);
    var isBilledOnCalendarMonths = subscription.getBillingInterval() != YEARLY;
    if (isBilledOnCalendarMonths) {
      sessionParamsBuilder.addLineItem(
          SessionCreateParams.LineItem.builder().setPrice(newVariableProductPrice.getId()).build());
    }
    if (isBilledOnCalendarMonths && startsAfterToday(billingCycleAnchor)) {
      sessionParamsBuilder.setSubscriptionData(
          SessionCreateParams.SubscriptionData.builder()
              .setBillingCycleAnchor(billingCycleAnchor)
              .setProrationBehavior(
                  SessionCreateParams.SubscriptionData.ProrationBehavior.CREATE_PRORATIONS)
              .build());
    }
    if (!isBilledOnCalendarMonths) {
      sessionParamsBuilder.setSubscriptionData(
          SessionCreateParams.SubscriptionData.builder()
              .putMetadata(OVERAGE_METERED_PRICE_ID_METADATA_KEY, newVariableProductPrice.getId())
              .putMetadata(
                  OVERAGE_BILLING_CYCLE_ANCHOR_METADATA_KEY, String.valueOf(billingCycleAnchor))
              .build());
    }
    return Session.create(
        sessionParamsBuilder
            .setSuccessUrl(redirectionUrls.getSuccessUrl())
            .setCancelUrl(redirectionUrls.getFailureUrl())
            .setUiMode(HOSTED)
            .setPaymentMethodCollection(IF_REQUIRED)
            .build());
  }

  private boolean startsAfterToday(Long billingCycleAnchor) {
    return Instant.ofEpochSecond(billingCycleAnchor)
        .atZone(ZoneId.of("Europe/Paris"))
        .toLocalDate()
        .isAfter(temporalUtils.today());
  }

  public void scheduleOverageSubscription(
      String stripeCustomerId, String meteredPriceId, Long billingCycleAnchor, User user) {
    if (hasScheduleOn(stripeCustomerId, meteredPriceId)) {
      log.info(
          "Overage schedule already created for StripeCustomer.id={} on Price.id={}, skipping",
          stripeCustomerId,
          meteredPriceId);
      return;
    }
    SubscriptionSchedule overageSchedule =
        overageScheduleCreation(stripeCustomerId, meteredPriceId, billingCycleAnchor);
    saveSubscriptionSession(user, overageSchedule.getId(), billingCycleAnchor);
  }

  @SneakyThrows
  private boolean hasScheduleOn(String stripeCustomerId, String meteredPriceId) {
    return SubscriptionSchedule.list(
            SubscriptionScheduleListParams.builder().setCustomer(stripeCustomerId).build())
        .getData()
        .stream()
        .filter(schedule -> schedule.getCanceledAt() == null)
        .flatMap(schedule -> schedule.getPhases().stream())
        .flatMap(phase -> phase.getItems().stream())
        .anyMatch(item -> meteredPriceId.equals(item.getPrice()));
  }

  private void saveSubscriptionSession(
      User user, String subscriptionScheduleId, long billingCycleAnchor) {
    userSubscriptionSessionRepository.save(
        UserSubscriptionSession.builder()
            .id(randomUUID().toString())
            .userId(user.getId())
            .subscriptionScheduleId(subscriptionScheduleId)
            .isCancelled(false)
            .trialUntil(
                Instant.ofEpochSecond(billingCycleAnchor)
                    .atZone(ZoneId.of("Europe/Paris"))
                    .toLocalDate())
            .build());
  }

  @SneakyThrows
  public SubscriptionSchedule overageScheduleCreation(
      String customerId, String meteredPriceId, long billingCycleAnchor) {
    var phase =
        SubscriptionScheduleCreateParams.Phase.builder()
            .addItem(
                SubscriptionScheduleCreateParams.Phase.Item.builder()
                    .setPrice(meteredPriceId)
                    .build())
            .setIterations(ANNUAL_OVERAGE_MONTHLY_ITERATIONS)
            .build();
    return SubscriptionSchedule.create(
        SubscriptionScheduleCreateParams.builder()
            .setCustomer(customerId)
            .setStartDate(billingCycleAnchor)
            .setEndBehavior(SubscriptionScheduleCreateParams.EndBehavior.CANCEL)
            .addPhase(phase)
            .build());
  }

  private SessionCreateParams.LineItem.PriceData.Recurring
      computeRecurringFromSubscriptionProductForSubscriptionMode(
          SubscriptionProduct subscriptionProduct) {
    if (Objects.requireNonNull(subscriptionProduct.getType()) == MONTHLY) {
      return SessionCreateParams.LineItem.PriceData.Recurring.builder()
          .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
          .build();
    }
    throw new IllegalArgumentException(
        "Unknown subscription type: " + subscriptionProduct.getType());
  }
}
