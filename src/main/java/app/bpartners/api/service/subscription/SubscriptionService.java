package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionType.ESSENTIAL;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.*;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static app.bpartners.api.payment.StripeConf.defaultCurrency;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionType;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.subscription.*;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionConsumptionLogJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionSessionRepository;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
  private static final long DEFAULT_SUBSCRIPTION_DELAY = 30L;
  private static final int DEFAULT_TRIAL_PERIOD_DAYS = 14;
  public static final long FREE_ROOF_ANALYSIS = 20L;
  private final StripeConf stripeConf;
  private final StripeClient stripeClient;
  private final UserRepository userRepository;
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepository;
  private final TemporalUtils temporalUtils;
  private final SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepository;
  private final StripeSessionFactory stripeSessionFactory;
  private final UserSubscriptionSessionRepository userSubscriptionSessionRepository;

  public SubscriptionConsumptionLog addConsumption(
      SubscriptionConsumptionLog subscriptionConsumptionLog) {
    return consumptionLogJpaRepository.save(subscriptionConsumptionLog);
  }

  public List<SubscriptionConsumptionLog> findConsumptionLogsByUserId(
      String userId, @Nullable Instant from, @Nullable Instant to) {
    var startOfMonth = temporalUtils.startOfMonth();
    var endOfMonth = temporalUtils.endOfMonth();
    return consumptionLogJpaRepository.findAllByUserIdAndCreationDatetimeBetween(
        userId, from == null ? startOfMonth : from, to == null ? endOfMonth : to);
  }

  public List<ConsumptionUsageSummary> computeMonthlySubscriptionVariableConsumption(User user) {
    var consumptionLogs =
        findConsumptionLogsByUserId(
            user.getId(), temporalUtils.startOfMonth(), temporalUtils.endOfMonth());
    return computeSubscriptionVariableConsumption(user, consumptionLogs);
  }

  @SneakyThrows
  private List<ConsumptionUsageSummary> computeSubscriptionVariableConsumption(
      User user, List<SubscriptionConsumptionLog> consumptionLogs) {
    var usageByTypes = calculateUsageByType(consumptionLogs);
    usageByTypes.forEach(
        consumptionUsageSummary -> {
          var actualUsage = consumptionUsageSummary.usage();
          var payableUsage = actualUsage - FREE_ROOF_ANALYSIS;
          if (payableUsage > 0) {
            SubscriptionItem subscriptionItemForProduct;
            try {
              subscriptionItemForProduct =
                  getSubscriptionItem(user, consumptionUsageSummary.consumptionType());
            } catch (StripeException e) {
              throw new ApiException(SERVER_EXCEPTION, e);
            }
            var subscriptionItemId = subscriptionItemForProduct.getId();
            var usageRecordCreateOnSubscriptionItemParams =
                UsageRecordCreateOnSubscriptionItemParams.builder()
                    .setQuantity(payableUsage)
                    .setTimestamp(now().getEpochSecond())
                    .build();
            try {
              UsageRecord.createOnSubscriptionItem(
                  subscriptionItemId, usageRecordCreateOnSubscriptionItemParams);
            } catch (StripeException e) {
              throw new ApiException(SERVER_EXCEPTION, e);
            }
          }
        });
    return usageByTypes;
  }

  private SubscriptionItem getSubscriptionItem(
      User user, SubscriptionConsumptionType consumptionType) throws StripeException {
    var subscriptionProduct =
        subscriptionProductRepository.findByConsumptionTypeAttached(consumptionType);
    var stripeSubscriptions =
        stripeClient
            .subscriptions()
            .list(
                SubscriptionListParams.builder().setCustomer(user.getUserSubscriptionId()).build())
            .getData();
    var latestSubscription =
        stripeSubscriptions.stream()
            .max(comparing(com.stripe.model.Subscription::getCurrentPeriodStart))
            .orElseThrow(
                () -> new NotFoundException("Any subscription found for User.id=" + user.getId()));
    var latestSubscriptionId = latestSubscription.getId();
    var subscriptionItems =
        stripeClient
            .subscriptionItems()
            .list(
                SubscriptionItemListParams.builder().setSubscription(latestSubscriptionId).build())
            .getData();
    return subscriptionItems.stream()
        .filter(
            subscriptionItem -> {
              var price = subscriptionItem.getPrice();
              var product = getProductById(price.getProduct());
              return product.getId().equals(subscriptionProduct.getE2Id());
            })
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Any SubscriptionItem matches to SubscriptionProduct for User.id="
                        + user.getId()));
  }

  @SneakyThrows
  private Product getProductById(String stripeProductId) {
    return stripeClient.products().retrieve(stripeProductId);
  }

  private List<ConsumptionUsageSummary> calculateUsageByType(
      List<SubscriptionConsumptionLog> logs) {
    if (logs == null || logs.isEmpty()) {
      return List.of();
    }

    var consumptionTypeLongMap =
        logs.stream()
            .collect(
                Collectors.groupingBy(
                    SubscriptionConsumptionLog::getConsumptionType,
                    Collectors.summingLong(SubscriptionConsumptionLog::getUsageMetric)));

    return consumptionTypeLongMap.entrySet().stream()
        .map(entry -> new ConsumptionUsageSummary(entry.getKey(), entry.getValue()))
        .toList();
  }

  public Subscription getBySubscriptionType(@NotNull UserSubscriptionType userSubscriptionType) {
    if (userSubscriptionType.equals(ESSENTIAL)) {
      var defaultSubscriptionProductId = stripeConf.getEssentialSubscriptionProductId();
      var subscriptionProduct =
          subscriptionProductRepository
              .findById(defaultSubscriptionProductId)
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          "SubscriptionProduct(id="
                              + defaultSubscriptionProductId
                              + ") not found"));
      return Subscription.builder()
          .subscriptionProduct(
              getSubscriptionProductByE2Id(
                  subscriptionProduct.getId(), subscriptionProduct.getE2Id()))
          .endDatetime(now().plus(DEFAULT_SUBSCRIPTION_DELAY, DAYS))
          .build();
    }
    throw new NotImplementedException("Only ESSENTIAL subscription type is supported");
  }

  @SneakyThrows
  public UserSubscription getSubscriptionByUserId(String userId) {
    var user = userRepository.getById(userId);
    return getSubscriptionByUser(user);
  }

  @SneakyThrows
  public UserSubscription getSubscriptionByUser(User user) {
    var optionalUserSubscriptionEligible =
        subscriptionEligibleJpaRepository.findByUserId(user.getId());
    if (optionalUserSubscriptionEligible.isPresent()) {
      var subscriptionEligible = optionalUserSubscriptionEligible.get();
      if (!subscriptionEligible.hasFreeTrialPeriodActive()) {
        var stripeCustomerId = user.getUserSubscriptionId();
        var subscriptions = getSubscriptionsFromStripeCustomer(stripeCustomerId);
        return UserSubscription.builder().user(user).subscriptions(subscriptions).build();
      }
    }
    return UserSubscription.builder().user(user).subscriptions(defaultActiveSubscription()).build();
  }

  private static @NotNull List<Subscription> defaultActiveSubscription() {
    Instant now = now();
    return List.of(
        Subscription.builder()
            .active(true)
            .status(TRIALING)
            .startDatetime(now)
            .endDatetime(now)
            .build());
  }

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
    // TODO persist subscriptionProduct here and return persisted domain subscription product id
    return fromStripeProduct(randomUUID().toString(), createdStripeProduct);
  }

  @SneakyThrows
  private SubscriptionProduct fromStripeProduct(
      String domainProductId, Product createdStripeProduct) {
    var createdDefaultPriceId = createdStripeProduct.getDefaultPrice();
    var price = stripeClient.prices().retrieve(createdDefaultPriceId);
    return SubscriptionProduct.builder()
        .id(domainProductId)
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
    if (Objects.requireNonNull(subscriptionType) == MONTHLY) {
      return ProductCreateParams.DefaultPriceData.Recurring.Interval.MONTH;
    }
    throw new IllegalArgumentException("Unknown subscription type " + subscriptionType);
  }

  @SneakyThrows
  public Redirection initiateSubscription(
      User user, Subscription subscription, RedirectionStatusUrls redirectionUrls) {
    @NotNull Customer stripeCustomer;
    try {
      stripeCustomer = getStripeCustomerByE2Id(user.getUserSubscriptionId());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "User.id=" + user.getId() + " is not associated to a stripe customer yet");
    }
    var actualUserSubscription = getSubscriptionByUser(user);
    var latestSubscription = actualUserSubscription.getLatestSubscription();
    if (latestSubscription != null
        && latestSubscription.isActive()
        && !(TRIALING).equals(latestSubscription.getStatus())) {
      throw new BadRequestException(
          "User.id="
              + user.getId()
              + " has active subscription until "
              + latestSubscription.getEndDatetime());
    }
    var endOfTrialPeriod = computeEndOfTrialPeriod(user);
    var billingCycleAnchor = computeBillingCycleAnchor(endOfTrialPeriod);
    log.info(
        "Schedule start date = {}",
        Instant.ofEpochSecond(billingCycleAnchor).atZone(ZoneId.of("Europe/Paris")).toLocalDate());

    var subscriptionProduct = subscription.getSubscriptionProduct();
    var subscriptionProductRoofAnalysis =
        subscriptionProductRepository.findByConsumptionTypeAttached(ROOF_ANALYSIS);
    var newVariableProductPrice =
        stripeClient
            .prices()
            .create(
                PriceCreateParams.builder()
                    .setCurrency(defaultCurrency())
                    .setProduct(subscriptionProductRoofAnalysis.getE2Id())
                    .setUnitAmount(200L)
                    .setRecurring(
                        PriceCreateParams.Recurring.builder()
                            .setUsageType(PriceCreateParams.Recurring.UsageType.METERED)
                            .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                            .build())
                    .build());

    var session =
        stripeSessionFactory.createSession(
            user,
            endOfTrialPeriod,
            stripeCustomer,
            subscriptionProduct,
            newVariableProductPrice,
            redirectionUrls,
            billingCycleAnchor,
            subscription);
    return new Redirection()
        .redirectionUrl(session.getUrl())
        .redirectionStatusUrls(
            new RedirectionStatusUrls()
                .successUrl(session.getSuccessUrl())
                .failureUrl(session.getCancelUrl()));
  }

  private LocalDate computeEndOfTrialPeriod(User user) {
    var userEligibility =
        subscriptionEligibleJpaRepository.findByUserId(user.getId()).orElseThrow();
    return userEligibility.getLatestTrialPeriodDate();
  }

  private Long computeBillingCycleAnchor(LocalDate latestTrialPeriodDate) {
    var nextBillingDate = temporalUtils.fifthOfNextMonth();
    if (latestTrialPeriodDate.isAfter(temporalUtils.endOfActualMonth())) {
      nextBillingDate = temporalUtils.fifthOfMonthAfter(2);
    }
    return Date.from(nextBillingDate.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant()).getTime()
        / 1000L;
  }

  @SneakyThrows
  @Transactional
  public UserSubscription createOrLinkUserSubscription(User user) {
    var optionalStripeCustomer = getStripeCustomerByEmail(user.getEmail());
    if (optionalStripeCustomer.isPresent()) {
      var customer = optionalStripeCustomer.get();
      List<Subscription> subscriptions = getSubscriptionsFromStripeCustomer(customer.getId());
      var savedUser =
          userRepository.save(user.toBuilder().userSubscriptionId(customer.getId()).build());
      var eligibleUser = makeUserEligibleIfNot(savedUser);
      return UserSubscription.builder().user(eligibleUser).subscriptions(subscriptions).build();
    }
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

    var eligibleUser = makeUserEligibleIfNot(savedUser);
    return UserSubscription.builder().user(eligibleUser).subscriptions(subscriptions).build();
  }

  private User makeUserEligibleIfNot(User user) {
    var optionalUserSubscriptionEligible =
        subscriptionEligibleJpaRepository.findByUserId(user.getId());
    if (optionalUserSubscriptionEligible.isPresent()) {
      return user;
    }
    subscriptionEligibleJpaRepository.save(
        UserSubscriptionEligible.builder()
            .id(randomUUID().toString())
            .userId(user.getId())
            .trialPeriodDays(DEFAULT_TRIAL_PERIOD_DAYS)
            .eligibleFrom(LocalDate.now())
            .creationDatetime(now())
            .build());
    return user;
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
              var currentPeriodStartLongValue = subscription.getCurrentPeriodStart();
              var startDatetime =
                  currentPeriodStartLongValue == null
                      ? null
                      : Instant.ofEpochSecond(currentPeriodStartLongValue);
              var currentPeriodEndLongValue = subscription.getCurrentPeriodEnd();
              var endDatetime =
                  currentPeriodEndLongValue == null
                      ? null
                      : Instant.ofEpochSecond(currentPeriodEndLongValue);
              var status = computeUserSubscriptionStatus(subscription);
              var paymentSettings = subscription.getPaymentSettings();
              return Subscription.builder()
                  .id(randomUUID().toString()) // TODO: update when subscription history persisted
                  .e2Id(subscription.getId())
                  .startDatetime(startDatetime)
                  .endDatetime(endDatetime)
                  .status(status)
                  .active(!status.equals(UNKNOWN))
                  .paymentMethods(
                      paymentSettings == null
                          ? new ArrayList<>()
                          : paymentSettings.getPaymentMethodTypes())
                  .build();
            })
        .toList();
  }

  private static Subscription.SubscriptionStatus computeUserSubscriptionStatus(
      com.stripe.model.Subscription subscription) {
    if (subscription.getCancelAtPeriodEnd() != null
        && subscription.getCancelAtPeriodEnd().equals(true)) {
      return CANCELED;
    }
    var subscriptionStatus = subscription.getStatus();
    return switch (subscriptionStatus) {
      case "active" -> ACTIVE;
      case "trialing" -> TRIALING;
      case "canceled" -> CANCELED;
      default -> {
        log.error("Unknown subscription status: {}", subscriptionStatus);
        yield UNKNOWN;
      }
    };
  }

  @SneakyThrows
  private @NotNull Customer getStripeCustomerByE2Id(String stripeCustomerId) {
    if (stripeCustomerId == null) {
      throw new IllegalArgumentException("Stripe customer id is mandatory and can not be null");
    }
    return stripeClient.customers().retrieve(stripeCustomerId);
  }

  @SneakyThrows
  private Optional<Customer> getStripeCustomerByEmail(String stripeCustomerEmail) {
    if (stripeCustomerEmail == null) {
      throw new IllegalArgumentException("Stripe customer id is mandatory and can not be null");
    }
    var customers =
        stripeClient
            .customers()
            .list(CustomerListParams.builder().setEmail(stripeCustomerEmail).build())
            .getData();
    return customers.stream().findFirst();
  }

  @SneakyThrows
  public UserSubscription cancelLatestUserSubscription(User user) {
    if (user.getUserSubscriptionId() == null) {
      throw new IllegalArgumentException(
          "User.userSubscriptionId is required to cancel subscription, "
              + "otherwise User.id="
              + user.getId()
              + " does not have userSubscriptionId");
    }

    var subscriptions = getSubscriptionsFromStripeCustomer(user.getUserSubscriptionId());
    if (subscriptions.isEmpty()) {
      throw new BadRequestException("User.id=" + user.getId() + " does not have any subscriptions");
    }
    var latestSubscription =
        subscriptions.stream()
            .sorted(comparing(Subscription::getStartDatetime, naturalOrder()).reversed())
            .toList()
            .getFirst();
    if (!latestSubscription.isActive()) {
      throw new IllegalStateException(
          "Only active subscription can be cancelled but actual status is "
              + latestSubscription.getStatus());
    }

    // TODO: check if subscription is type SETUP
    /**
     * if true: cancel subscriptionScheduler persist user whom cancel subscription during setup
     * (userId, date) return what stripe return
     */
    List<UserSubscriptionSession> userSubscriptionSessions =
        userSubscriptionSessionRepository.findAllByUserId(user.getId()).stream()
            .filter(
                userSubscriptionSession ->
                    userSubscriptionSession.getSetUpUntil().isAfter(LocalDate.now()))
            .filter(UserSubscriptionSession::isCancelled)
            .toList();
    if (!userSubscriptionSessions.isEmpty()) {
      UserSubscriptionSession userInSetUpMode =
          userSubscriptionSessions.stream()
              .filter(
                  userSubscriptionSession ->
                      userSubscriptionSession.getUserId().equals(user.getId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Aucune session trouvée pour l'utilisateur " + user.getId()));

      userSubscriptionSessionRepository.save(userInSetUpMode.toBuilder().isCancelled(true).build());
      SubscriptionSchedule resource =
          SubscriptionSchedule.retrieve(userInSetUpMode.getSubscriptionScheduleId());
      resource.cancel();
      return UserSubscription.builder().user(user).build();
    }
    stripeClient
        .subscriptions()
        .update(
            latestSubscription.getE2Id(),
            SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build());

    var actualSubscriptions = getSubscriptionsFromStripeCustomer(user.getUserSubscriptionId());
    return UserSubscription.builder().user(user).subscriptions(actualSubscriptions).build();
  }

  @SneakyThrows
  public User deleteUserFromStripe(User user) {
    if (user.getUserSubscriptionId() == null) {
      throw new IllegalArgumentException(
          "User.userSubscriptionId is required to remove stripe customer, "
              + "otherwise User.id="
              + user.getId()
              + " does not have userSubscriptionId");
    }
    stripeClient.customers().delete(user.getUserSubscriptionId());

    return userRepository.save(user.toBuilder().userSubscriptionId(null).build());
  }

  private SubscriptionType computeTypeFromRecurring(String intervalValue) {
    if (intervalValue.equals("month")) {
      return MONTHLY;
    }
    throw new IllegalArgumentException(
        "Unknown or not supported subscription type: " + intervalValue);
  }

  @SneakyThrows
  public UserSubscription getSubscriptionByUserSubscriptionId(String stripeCustomerId) {
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
  public SubscriptionProduct getSubscriptionProductByE2Id(String domainProductId, String e2Id) {
    return fromStripeProduct(domainProductId, stripeClient.products().retrieve(e2Id));
  }
}
