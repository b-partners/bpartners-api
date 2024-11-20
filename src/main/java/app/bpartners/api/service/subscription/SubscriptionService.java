package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static app.bpartners.api.model.subscription.SubscriptionType.YEARLY;
import static app.bpartners.api.payment.StripeConf.defaultCurrency;
import static com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION;
import static com.stripe.param.checkout.SessionCreateParams.SubscriptionData.TrialSettings.EndBehavior.MissingPaymentMethod.CANCEL;
import static com.stripe.param.checkout.SessionCreateParams.UiMode.HOSTED;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.SubscriptionType;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.repository.UserRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
  private final StripeClient stripeClient;
  private final UserRepository userRepository;

  @SneakyThrows
  public SubscriptionProduct createSubscriptionProduct(SubscriptionProduct subscriptionProduct) {
    var productCreateParams =
        ProductCreateParams.builder()
            .setName(subscriptionProduct.getName())
            .setDescription(subscriptionProduct.getDescription())
            .addAllMarketingFeature(
                subscriptionProduct.getFeatures().stream()
                    .map(
                        feature ->
                            ProductCreateParams.MarketingFeature.builder().setName(feature).build())
                    .toList())
            .addImage(subscriptionProduct.getImageUrl())
            .setActive(true)
            .setDefaultPriceData(
                ProductCreateParams.DefaultPriceData.builder()
                    .setCurrency(defaultCurrency())
                    .setUnitAmount(subscriptionProduct.getPriceInCents())
                    .setRecurring(
                        ProductCreateParams.DefaultPriceData.Recurring.builder()
                            .setInterval(
                                intervalFromSubscriptionType(subscriptionProduct.getType()))
                            .build())
                    .build())
            .build();
    var createdStripeProduct = stripeClient.products().create(productCreateParams);
    return fromStripeProduct(createdStripeProduct);
  }

  @SneakyThrows
  private SubscriptionProduct fromStripeProduct(Product createdStripeProduct) {
    var createdDefaultPriceId = createdStripeProduct.getDefaultPrice();
    var price = stripeClient.prices().retrieve(createdDefaultPriceId);
    return SubscriptionProduct.builder()
        .id(randomUUID().toString())
        .e2Id(createdStripeProduct.getId())
        .name(createdStripeProduct.getName())
        .description(createdStripeProduct.getDescription())
        .features(
            createdStripeProduct.getMarketingFeatures().stream()
                .map(Product.MarketingFeature::getName)
                .toList())
        .priceInCents(price.getUnitAmount())
        .imageUrl(createdStripeProduct.getImages().getFirst())
        .type(computeTypeFromRecurring(price.getRecurring().getInterval()))
        .creationDatetime(Instant.ofEpochSecond(createdStripeProduct.getCreated()))
        .build();
  }

  private ProductCreateParams.DefaultPriceData.Recurring.Interval intervalFromSubscriptionType(
      SubscriptionType subscriptionType) {
    switch (subscriptionType) {
      case MONTHLY -> {
        return ProductCreateParams.DefaultPriceData.Recurring.Interval.MONTH;
      }
      case YEARLY -> {
        return ProductCreateParams.DefaultPriceData.Recurring.Interval.YEAR;
      }
      default -> throw new IllegalArgumentException(
          "Unknown subscription type " + subscriptionType);
    }
  }

  @SneakyThrows
  public Redirection initiateSubscription(
      User user, Subscription subscription, RedirectionStatusUrls redirectionUrls) {
    var stripeCustomer = getStripeCustomerByE2Id(user.getUserSubscriptionId());
    var subscriptionProduct = subscription.getSubscriptionProduct();
    var subscriptionBuilder =
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
                                computeRecurringFromSubscriptionProduct(subscriptionProduct))
                            .build())
                    .build())
            .setSuccessUrl(redirectionUrls.getSuccessUrl())
            .setCancelUrl(redirectionUrls.getFailureUrl())
            .setUiMode(HOSTED);
    if (subscription.hasFreeTrialPeriod()) {
      subscriptionBuilder.setSubscriptionData(
          SessionCreateParams.SubscriptionData.builder()
              .setTrialSettings(
                  SessionCreateParams.SubscriptionData.TrialSettings.builder()
                      .setEndBehavior(
                          SessionCreateParams.SubscriptionData.TrialSettings.EndBehavior.builder()
                              .setMissingPaymentMethod(CANCEL)
                              .build())
                      .build())
              .setTrialPeriodDays(subscription.getFreeTrialDays())
              .build());
    }
    Session session = Session.create(subscriptionBuilder.build());
    return new Redirection()
        .redirectionUrl(session.getUrl())
        .redirectionStatusUrls(
            new RedirectionStatusUrls()
                .successUrl(session.getSuccessUrl())
                .failureUrl(session.getCancelUrl()));
  }

  @SneakyThrows
  public UserSubscription createUserSubscription(User user) {
    var defaultHolder = user.getDefaultHolder();
    var customerCreateParams =
        CustomerCreateParams.builder()
            .setName(user.getName())
            .setEmail(user.getEmail())
            .setPhone(user.getMobilePhoneNumber())
            .setAddress(
                CustomerCreateParams.Address.builder()
                    .setCountry(defaultHolder.getCountry())
                    .setCity(defaultHolder.getCity())
                    .setLine1(defaultHolder.getAddress())
                    .setPostalCode(defaultHolder.getPostalCode())
                    .build())
            .build();
    var createdStripeCustomer = stripeClient.customers().create(customerCreateParams);
    var savedUser =
        userRepository.save(
            user.toBuilder().userSubscriptionId(createdStripeCustomer.getId()).build());
    var subscriptions = getSubscriptionsFromStripeCustomer(createdStripeCustomer.getId());

    return UserSubscription.builder().user(savedUser).subscriptions(subscriptions).build();
  }

  @SneakyThrows
  public UserSubscription updateUserSubscription(User user) {
    if (user.getUserSubscriptionId() == null) {
      throw new IllegalArgumentException(
          "User.userSubscriptionId is required to update subscription, "
              + "otherwise User.id="
              + user.getId()
              + " does not have userSubscriptionId");
    }
    var defaultHolder = user.getDefaultHolder();
    var customerUpdateParams =
        CustomerUpdateParams.builder()
            .setName(user.getName())
            .setEmail(user.getEmail())
            .setPhone(user.getMobilePhoneNumber())
            .setAddress(
                CustomerUpdateParams.Address.builder()
                    .setCountry(defaultHolder.getCountry())
                    .setCity(defaultHolder.getCity())
                    .setLine1(defaultHolder.getAddress())
                    .setPostalCode(defaultHolder.getPostalCode())
                    .build())
            .build();
    var updatedStripeCustomer =
        stripeClient.customers().update(user.getUserSubscriptionId(), customerUpdateParams);
    var subscriptions = getSubscriptionsFromStripeCustomer(updatedStripeCustomer.getId());
    return UserSubscription.builder().user(user).subscriptions(subscriptions).build();
  }

  private @NotNull List<Subscription> getSubscriptionsFromStripeCustomer(String stripeCustomerId)
      throws StripeException {
    var stripeSubscriptions =
        stripeClient
            .subscriptions()
            .list(SubscriptionListParams.builder().setCustomer(stripeCustomerId).build())
            .getData();
    return stripeSubscriptions.stream()
        .map(
            subscription -> {
              var trialEnd = Instant.ofEpochSecond(subscription.getTrialEnd());
              var trialStart = Instant.ofEpochSecond(subscription.getTrialStart());
              var freeTrialDays =
                  (trialEnd == null || trialStart == null) ? 0L : trialStart.until(trialEnd, DAYS);
              var startDatetime = Instant.ofEpochSecond(subscription.getCurrentPeriodStart());
              var endDatetime = Instant.ofEpochSecond(subscription.getCurrentPeriodEnd());
              return Subscription.builder()
                  .id(randomUUID().toString()) // TODO: update when subscription history persisted
                  .e2Id(subscription.getId())
                  .startDatetime(startDatetime)
                  .endDatetime(endDatetime)
                  .freeTrialDays(freeTrialDays)
                  .freeTrialStart(trialStart)
                  .freeTrialEnd(trialEnd)
                  .active(subscription.getStatus().equals(SubscriptionStatus.ACTIVE.name()))
                  .paymentMethods(subscription.getPaymentSettings().getPaymentMethodTypes())
                  .build();
            })
        .toList();
  }

  @SneakyThrows
  private @NotNull Customer getStripeCustomerByE2Id(String stripeCustomerId) {
    if (stripeCustomerId == null) {
      throw new IllegalArgumentException("Stripe customer id is mandatory and can not be null");
    }
    return stripeClient.customers().retrieve(stripeCustomerId);
  }

  @SneakyThrows
  public UserSubscription cancelUserSubscription(User user) {
    if (user.getUserSubscriptionId() == null) {
      throw new IllegalArgumentException(
          "User.userSubscriptionId is required to update subscription, "
              + "otherwise User.id="
              + user.getId()
              + " does not have userSubscriptionId");
    }
    var subscriptions = getSubscriptionsFromStripeCustomer(user.getUserSubscriptionId());

    subscriptions.forEach(
        subscription -> {
          try {
            stripeClient.subscriptions().cancel(subscription.getId());
          } catch (StripeException e) {
            throw new ApiException(SERVER_EXCEPTION, e);
          }
        });
    stripeClient.customers().delete(user.getUserSubscriptionId());

    var savedUser = userRepository.save(user.toBuilder().userSubscriptionId(null).build());

    return UserSubscription.builder().user(savedUser).subscriptions(new ArrayList<>()).build();
  }

  private SessionCreateParams.LineItem.PriceData.Recurring computeRecurringFromSubscriptionProduct(
      SubscriptionProduct subscriptionProduct) {
    switch (subscriptionProduct.getType()) {
      case MONTHLY -> {
        return SessionCreateParams.LineItem.PriceData.Recurring.builder()
            .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
            .build();
      }
      case YEARLY -> {
        return SessionCreateParams.LineItem.PriceData.Recurring.builder()
            .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR)
            .build();
      }
      default -> throw new IllegalArgumentException(
          "Unknown subscription type: " + subscriptionProduct.getType());
    }
  }

  private SubscriptionType computeTypeFromRecurring(String intervalValue) {
    switch (intervalValue) {
      case "month" -> {
        return MONTHLY;
      }
      case "year" -> {
        return YEARLY;
      }
      default -> throw new IllegalArgumentException(
          "Unknown or not supported subscription type: " + intervalValue);
    }
  }

  @SneakyThrows
  public UserSubscription findUserSubscriptionByCriteria(String stripeCustomerId) {
    var stripeCustomer = stripeClient.customers().retrieve(stripeCustomerId);
    var user =
        userRepository
            .findByEmail(stripeCustomer.getEmail())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Unable to found User with email "
                            + stripeCustomer.getEmail()
                            + ") "
                            + "associated to StripeCustomer.id="
                            + stripeCustomer.getId()));
    try {
      return UserSubscription.builder()
          .user(user)
          .subscriptions(getSubscriptionsFromStripeCustomer(stripeCustomer.getId()))
          .build();
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @SneakyThrows
  public SubscriptionProduct getSubscriptionProductByE2Id(String e2Id) {
    return fromStripeProduct(stripeClient.products().retrieve(e2Id));
  }

  enum SubscriptionStatus {
    ACTIVE
  }
}
