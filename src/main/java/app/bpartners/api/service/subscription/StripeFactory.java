package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static app.bpartners.api.payment.StripeConf.defaultCurrency;
import static com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION;
import static com.stripe.param.checkout.SessionCreateParams.PaymentMethodCollection.IF_REQUIRED;
import static com.stripe.param.checkout.SessionCreateParams.UiMode.HOSTED;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeFactory {
  private final TemporalUtils temporalUtils;

  public List<com.stripe.model.Subscription> retrieveUserSubscriptions(User user)
      throws StripeException {
    SubscriptionListParams params =
        SubscriptionListParams.builder().setCustomer(user.getUserSubscriptionId()).build();
    return com.stripe.model.Subscription.list(params).getData();
  }

  public Redirection initiateSubscriptionWorkflow(
      Customer stripeCustomer,
      RedirectionStatusUrls redirectionUrls,
      long billingCycleAnchor,
      Subscription subscription)
      throws StripeException {
    var sessionSubscription =
        createSessionSubscription(
            stripeCustomer, subscription, redirectionUrls, billingCycleAnchor);
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
    if (isBilledOnCalendarMonths && startsAfterToday(billingCycleAnchor)) {
      sessionParamsBuilder.setSubscriptionData(
          SessionCreateParams.SubscriptionData.builder()
              .setBillingCycleAnchor(billingCycleAnchor)
              .setProrationBehavior(
                  SessionCreateParams.SubscriptionData.ProrationBehavior.CREATE_PRORATIONS)
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
