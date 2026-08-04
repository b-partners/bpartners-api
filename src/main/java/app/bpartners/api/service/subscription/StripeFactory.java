package app.bpartners.api.service.subscription;

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
import com.stripe.param.checkout.SessionCreateParams;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
  private final TemporalUtils temporalUtils;
  private final UserSubscriptionSessionRepository userSubscriptionSessionRepository;

  public List<com.stripe.model.Subscription> retrieveUserSubscriptions(User user)
      throws StripeException {
    SubscriptionListParams params =
        SubscriptionListParams.builder().setCustomer(user.getUserSubscriptionId()).build();
    return com.stripe.model.Subscription.list(params).getData();
  }

  public Redirection initiateSubscriptionWorkflow(
      User user,
      LocalDate dateFromWhenSubscriptionPeriodStart,
      Customer stripeCustomer,
      SubscriptionProduct subscriptionProduct,
      Price price,
      RedirectionStatusUrls redirectionUrls,
      long billingCycleAnchor,
      Subscription subscription)
      throws StripeException {
    var today = LocalDate.now();
    boolean isTodayBeforeFifthOfActualMonth = today.isBefore(temporalUtils.fifthOfActualMonth());
    boolean isTrialEndBetweenFirstAndFifthOfActualMonth =
        (dateFromWhenSubscriptionPeriodStart.isAfter(temporalUtils.startOfActualMonth())
                || dateFromWhenSubscriptionPeriodStart.isEqual(temporalUtils.startOfActualMonth()))
            && (dateFromWhenSubscriptionPeriodStart.isBefore(temporalUtils.fifthOfActualMonth())
                || dateFromWhenSubscriptionPeriodStart.isEqual(temporalUtils.fifthOfActualMonth()));
    boolean isTrialEndBetweenFirstAndFifthOfNextMonth =
        (dateFromWhenSubscriptionPeriodStart.isAfter(temporalUtils.startOfNextMonth())
                || dateFromWhenSubscriptionPeriodStart.isEqual(temporalUtils.startOfNextMonth()))
            && (dateFromWhenSubscriptionPeriodStart.isBefore(temporalUtils.fifthOfNextMonth())
                || dateFromWhenSubscriptionPeriodStart.isBefore(temporalUtils.fifthOfNextMonth()));
    if (isTrialEndBetweenFirstAndFifthOfNextMonth || isTrialEndBetweenFirstAndFifthOfActualMonth) {
      scheduleSubscription(stripeCustomer, subscription, price, billingCycleAnchor, user);
      return new Redirection()
          .redirectionUrl(
              getDashboardUrl()
                  + "/account/"
                  + user.getDefaultAccount().getId()
                  + "?stripeStatus=done")
          .redirectionStatusUrls(redirectionUrls);
    } else if (dateFromWhenSubscriptionPeriodStart.isAfter(temporalUtils.fifthOfActualMonth())
        && dateFromWhenSubscriptionPeriodStart.isBefore(temporalUtils.endOfActualMonth())
        && !isTodayBeforeFifthOfActualMonth) {
      var sessionSubscription =
          createSessionSubscription(
              stripeCustomer, subscriptionProduct, price, redirectionUrls, billingCycleAnchor);
      return mapFromSession(sessionSubscription, redirectionUrls);
    }
    scheduleSubscription(stripeCustomer, subscription, price, billingCycleAnchor, user);
    return new Redirection()
        .redirectionUrl(
            getDashboardUrl()
                + "/account/"
                + user.getDefaultAccount().getId()
                + "?stripeStatus=done")
        .redirectionStatusUrls(redirectionUrls);
  }

  private String getDashboardUrl() {
    return System.getenv("ENV") != null && System.getenv("ENV").equals("preprod")
        ? "https://preprod.dashboard.birdia.fr"
        : "https://dashboard.birdia.fr";
  }

  private Redirection mapFromSession(Session session, RedirectionStatusUrls redirectionUrls) {
    return new Redirection()
        .redirectionUrl(session.getUrl())
        .redirectionStatusUrls(redirectionUrls);
  }

  public Session createSessionSubscription(
      Customer stripeCustomer,
      SubscriptionProduct subscriptionProduct,
      Price newVariableProductPrice,
      RedirectionStatusUrls redirectionUrls,
      Long billingCycleAnchor)
      throws StripeException {
    return Session.create(
        SessionCreateParams.builder()
            .setMode(SUBSCRIPTION)
            .setCustomer(stripeCustomer.getId())
            .setCurrency(defaultCurrency())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
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
                    .build())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPrice(newVariableProductPrice.getId())
                    .build())
            .setSuccessUrl(redirectionUrls.getSuccessUrl())
            .setCancelUrl(redirectionUrls.getFailureUrl())
            .setUiMode(HOSTED)
            .setSubscriptionData(
                SessionCreateParams.SubscriptionData.builder()
                    .setProrationBehavior(
                        SessionCreateParams.SubscriptionData.ProrationBehavior.NONE)
                    .setBillingCycleAnchor(billingCycleAnchor)
                    .build())
            .setPaymentMethodCollection(IF_REQUIRED)
            .build());
  }

  public void scheduleSubscription(
      Customer stripeCustomer,
      Subscription subscription,
      Price newVariableProductPrice,
      Long billingCycleAnchor,
      User user) {
    SubscriptionSchedule subscriptionSchedule =
        subscriptionScheduleCreation(
            stripeCustomer.getId(),
            subscription,
            newVariableProductPrice.getId(),
            billingCycleAnchor);

    userSubscriptionSessionRepository.save(
        UserSubscriptionSession.builder()
            .id(randomUUID().toString())
            .userId(user.getId())
            .subscriptionScheduleId(subscriptionSchedule.getId())
            .isCancelled(false)
            .trialUntil(
                Instant.ofEpochSecond(billingCycleAnchor)
                    .atZone(ZoneId.of("Europe/Paris"))
                    .toLocalDate())
            .build());
  }

  @SneakyThrows
  public SubscriptionSchedule subscriptionScheduleCreation(
      String customerId,
      Subscription subscription,
      String meteredPriceId,
      long billingCycleAnchor) {

    var phases = new ArrayList<SubscriptionScheduleCreateParams.Phase>();
    var recurringParams =
        computeRecurringFromSubscriptionProductForSetUpMode(subscription.getSubscriptionProduct());

    var basePlanItems =
        List.of(
            SubscriptionScheduleCreateParams.Phase.Item.builder()
                .setPriceData(
                    SubscriptionScheduleCreateParams.Phase.Item.PriceData.builder()
                        .setCurrency(defaultCurrency())
                        .setProduct(subscription.getSubscriptionProduct().getE2Id())
                        .setRecurring(recurringParams)
                        .setUnitAmount(
                            subscription.getSubscriptionProduct().getPriceInCentsWithVat())
                        .build())
                .build(),
            SubscriptionScheduleCreateParams.Phase.Item.builder().setPrice(meteredPriceId).build());

    phases.add(SubscriptionScheduleCreateParams.Phase.builder().addAllItem(basePlanItems).build());

    return SubscriptionSchedule.create(
        SubscriptionScheduleCreateParams.builder()
            .setCustomer(customerId)
            .setStartDate(billingCycleAnchor)
            .addAllPhase(phases)
            .build());
  }

  public SubscriptionScheduleCreateParams.Phase.Item.PriceData.Recurring
      computeRecurringFromSubscriptionProductForSetUpMode(SubscriptionProduct subscriptionProduct) {
    if (Objects.requireNonNull(subscriptionProduct.getType()) == MONTHLY) {
      return SubscriptionScheduleCreateParams.Phase.Item.PriceData.Recurring.builder()
          .setInterval(
              SubscriptionScheduleCreateParams.Phase.Item.PriceData.Recurring.Interval.MONTH)
          .build();
    }
    throw new IllegalArgumentException(
        "Unknown subscription type: " + subscriptionProduct.getType());
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
