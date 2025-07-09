package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static app.bpartners.api.payment.StripeConf.defaultCurrency;
import static com.stripe.param.checkout.SessionCreateParams.Mode.SETUP;
import static com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION;
import static com.stripe.param.checkout.SessionCreateParams.UiMode.HOSTED;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SessionMode;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionSession;
import app.bpartners.api.repository.jpa.UserSubscriptionSessionRepository;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.model.checkout.Session;
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
public class StripeSessionFactory {
  private final TemporalUtils temporalUtils;
  private final UserSubscriptionSessionRepository userSubscriptionSessionRepository;

  public Session createSession(
      User user,
      LocalDate trialEnd,
      Customer stripeCustomer,
      SubscriptionProduct subscriptionProduct,
      Price price,
      RedirectionStatusUrls redirectionUrls,
      long billingCycleAnchor,
      Subscription subscription)
      throws StripeException {
    boolean isTrialEndBetweenFirstAndFourthOfActualMonth =
        (trialEnd.isAfter(temporalUtils.startOfActualMonth())
                || trialEnd.isEqual(temporalUtils.startOfActualMonth()))
            && (trialEnd.isBefore(temporalUtils.fourthOfActualMonth())
                || trialEnd.isEqual(temporalUtils.fourthOfActualMonth()));
    boolean isTrialEndBetweenFirstAndFourthOfNextMonth =
        (trialEnd.isAfter(temporalUtils.startOfNextMonth())
                || trialEnd.isEqual(temporalUtils.startOfNextMonth()))
            && (trialEnd.isBefore(temporalUtils.fourthOfNextMonth())
                || trialEnd.isBefore(temporalUtils.fourthOfNextMonth()));
    if (isTrialEndBetweenFirstAndFourthOfNextMonth
        || isTrialEndBetweenFirstAndFourthOfActualMonth) {
      return createSessionSetUp(
          stripeCustomer, redirectionUrls, subscription, price, billingCycleAnchor, user);
    }
    if (trialEnd.isBefore(temporalUtils.endOfActualMonth())) {
      return createSessionSubscription(
          stripeCustomer, subscriptionProduct, price, redirectionUrls, billingCycleAnchor);
    } else {
      return createSessionSetUp(
          stripeCustomer, redirectionUrls, subscription, price, billingCycleAnchor, user);
    }
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
                            .setUnitAmount(subscriptionProduct.getPriceInCents())
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
            .build());
  }

  public Session createSessionSetUp(
      Customer stripeCustomer,
      RedirectionStatusUrls redirectionUrls,
      Subscription subscription,
      Price newVariableProductPrice,
      Long billingCycleAnchor,
      User user)
      throws StripeException {
    var session =
        Session.create(
            SessionCreateParams.builder()
                .setMode(SETUP)
                .setCustomer(stripeCustomer.getId())
                .setCurrency(defaultCurrency())
                .setSuccessUrl(redirectionUrls.getSuccessUrl())
                .setCancelUrl(redirectionUrls.getFailureUrl())
                .setUiMode(HOSTED)
                .build());
    SubscriptionSchedule subscriptionSchedule =
        subscriptionScheduleCreation(
            stripeCustomer.getId(),
            subscription,
            newVariableProductPrice.getId(),
            billingCycleAnchor);
    userSubscriptionSessionRepository.save(
        UserSubscriptionSession.builder()
            .id(randomUUID().toString())
            .sessionId(session.getId())
            .sessionMode(SessionMode.SETUP)
            .userId(user.getId())
            .subscriptionScheduleId(subscriptionSchedule.getId())
            .isCancelled(false)
            .trialUntil(
                Instant.ofEpochSecond(billingCycleAnchor)
                    .atZone(ZoneId.of("Europe/Paris"))
                    .toLocalDate())
            .build());

    return session;
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
                        .setUnitAmount(subscription.getSubscriptionProduct().getPriceInCents())
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
