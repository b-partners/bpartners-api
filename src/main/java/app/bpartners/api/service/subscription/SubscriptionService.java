package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.SubscriptionCancellationType.END_OF_PERIOD;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCancellationType.IMMEDIATE;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionType.ESSENTIAL;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.*;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static app.bpartners.api.payment.StripeConf.defaultCurrency;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.SubscriptionCancellationType;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionType;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.UserSubscriptionCommitmentAutoRenewalStatusHistory;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.subscription.*;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import app.bpartners.api.repository.jpa.*;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.StripeClient;
import com.stripe.exception.InvalidRequestException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
  private static final long DEFAULT_SUBSCRIPTION_DELAY = 30L;
  private static final int DEFAULT_TRIAL_PERIOD_DAYS = 7;
  private static final int DEFAULT_PLANS_PAGE_SIZE = 100;
  private static final String VAT_PERCENT_METADATA_KEY = "vat_percent";
  private static final String CANCEL_AFTER_FIRST_INVOICE_METADATA_KEY =
      "cancel_after_first_invoice";
  private static final String STRIPE_YEARLY_INTERVAL = "year";
  private static final SubscriptionCancellationType DEFAULT_CANCELLATION_TYPE = IMMEDIATE;
  private final StripeConf stripeConf;
  private final StripeClient stripeClient;
  private final UserRepository userRepository;
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepository;
  private final TemporalUtils temporalUtils;
  private final SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepository;
  private final StripeFactory stripeFactory;
  private final DetectionTrackingJpaRepository detectionTrackingJpaRepository;
  private final StripeInvoiceService stripeInvoiceService;
  private final StripeCustomerService stripeCustomerService;
  private final StripeSubscriptionService stripeSubscriptionService;
  private final UserSubscriptionProductService userSubscriptionProductService;
  private final UserSubscriptionCommitmentJpaRepository userSubscriptionCommitmentJpaRepository;
  private final UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository
      userSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository;

  public SubscriptionConsumptionLog addConsumption(
      SubscriptionConsumptionLog subscriptionConsumptionLog) {
    return consumptionLogJpaRepository.save(subscriptionConsumptionLog);
  }

  public List<SubscriptionConsumptionLog> findConsumptionLogsByUserId(
      String userId, @Nullable Instant from, @Nullable Instant to) {
    var startOfMonth = temporalUtils.startOfMonth();
    var endOfMonth = temporalUtils.endOfMonth();
    return detectionTrackingJpaRepository
        .findAllByIdUserAndCreationDatetimeBetween(
            userId, from == null ? startOfMonth : from, to == null ? endOfMonth : to)
        .stream()
        .map(
            tracking ->
                SubscriptionConsumptionLog.builder()
                    .id(tracking.getId())
                    .userId(userId)
                    .consumptionType(ROOF_ANALYSIS)
                    .usageMetric(1L)
                    .comment(
                        "Adresse : "
                            + tracking.getAddress()
                            + " - Initiateur : "
                            + tracking.getInitiatorName()
                            + " - "
                            + tracking.getInitiatorEmail()
                            + " - "
                            + tracking.getInitiatorPhoneNumber())
                    .creationDatetime(tracking.getCreationDatetime())
                    .consumptionUnit(UNIT)
                    .build())
        .toList();
  }

  public List<ConsumptionUsageSummary> computeMonthlySubscriptionVariableConsumption(User user) {
    var consumptionLogs =
        findConsumptionLogsByUserId(
            user.getId(),
            temporalUtils.startOfLastMonthInstant(),
            temporalUtils.endOfLastMonthInstant());
    return calculateUsageByType(consumptionLogs);
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

  public List<SubscriptionProduct> getSubscribablePlans(
      PageFromOne page, BoundedPageSize pageSize) {
    var pageValue = page != null ? page.getValue() - 1 : 0;
    var pageSizeValue = pageSize != null ? pageSize.getValue() : DEFAULT_PLANS_PAGE_SIZE;
    return subscriptionProductRepository.findAllByBillingTypeNotNull(
        PageRequest.of(pageValue, pageSizeValue));
  }

  public Subscription getBySubscriptionType(@NotNull UserSubscriptionType userSubscriptionType) {
    if (userSubscriptionType.equals(ESSENTIAL)) {
      return getByPlanId(stripeConf.getEssentialSubscriptionProductId(), BillingInterval.MONTHLY);
    }
    throw new NotImplementedException("Only ESSENTIAL subscription type is supported");
  }

  public Subscription getByPlanId(String planId, BillingInterval billingInterval) {
    var interval = billingInterval == null ? BillingInterval.MONTHLY : billingInterval;
    var subscriptionProduct =
        subscriptionProductRepository
            .findById(planId)
            .orElseThrow(
                () -> new NotFoundException("SubscriptionProduct(id=" + planId + ") not found"));
    if (interval == BillingInterval.YEARLY && !subscriptionProduct.hasAnnualPricing()) {
      throw new BadRequestException(
          "SubscriptionProduct(id=" + planId + ") has no annual pricing, YEARLY is not available");
    }
    return Subscription.builder()
        .subscriptionProduct(
            getSubscriptionProductByE2Id(
                subscriptionProduct.getId(), subscriptionProduct.getE2Id()))
        .billingInterval(interval)
        .endDatetime(now().plus(DEFAULT_SUBSCRIPTION_DELAY, DAYS))
        .build();
  }

  public List<UserSubscriptionCommitment> saveUserSubscriptionCommitments(
      List<UserSubscriptionCommitment> userSubscriptionCommitments) {
    return userSubscriptionCommitmentJpaRepository.saveAll(userSubscriptionCommitments);
  }

  public List<UserSubscriptionCommitment> getUserSubscriptionCommitments(String userIdentifier) {
    return userSubscriptionCommitmentJpaRepository.findAllByUserId(userIdentifier);
  }

  @Transactional
  public UserSubscriptionCommitment updateUserSubscriptionCommitmentAutoRenewalStatus(
      String userId, String commitmentId, EnableStatus autoRenewalStatus) {
    if (autoRenewalStatus == null) {
      throw new BadRequestException("autoRenewalStatus is mandatory.");
    }
    var commitment =
        userSubscriptionCommitmentJpaRepository
            .findById(commitmentId)
            .filter(it -> userId.equals(it.getUserId()))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "UserSubscriptionCommitment.id="
                            + commitmentId
                            + " not found for User.id="
                            + userId));
    userSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository.save(
        UserSubscriptionCommitmentAutoRenewalStatusHistory.builder()
            .id(randomUUID().toString())
            .userSubscriptionCommitmentId(commitmentId)
            .autoRenewalStatus(autoRenewalStatus)
            .creationDatetime(now())
            .build());
    return userSubscriptionCommitmentJpaRepository.findById(commitmentId).orElseThrow();
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
      var stripeCustomerId = user.getUserSubscriptionId();
      var subscriptions = getSubscriptionsFromStripeCustomer(stripeCustomerId);
      if (!subscriptionEligible.hasFreeTrialPeriodActive()
          || (subscriptionEligible.hasFreeTrialPeriodActive() && !subscriptions.isEmpty())) {
        return UserSubscription.builder().user(user).subscriptions(subscriptions).build();
      }
    }
    return UserSubscription.builder().user(user).subscriptions(defaultActiveSubscription()).build();
  }

  private static @NotNull List<Subscription> defaultActiveSubscription(
      Subscription.SubscriptionStatus subscriptionStatus, Instant start, Instant end) {
    return List.of(
        Subscription.builder()
            .active(true)
            .status(subscriptionStatus)
            .startDatetime(start)
            .endDatetime(end)
            .build());
  }

  private static @NotNull List<Subscription> defaultActiveSubscription() {
    Instant now = now();
    return defaultActiveSubscription(
        TRIALING,
        now,
        new TemporalUtils()
            .startOfNextMonth()
            .atStartOfDay(ZoneId.of("Europe/Paris"))
            .minusSeconds(1L)
            .toInstant());
  }

  @SneakyThrows
  public SubscriptionProduct createSubscriptionProduct(SubscriptionProduct subscriptionProduct) {
    var productCreateParamsBuilder =
        ProductCreateParams.builder()
            .setName(subscriptionProduct.getName())
            .setDescription(subscriptionProduct.getDescription())
            .addAllMarketingFeature(
                subscriptionProduct.getFeatures().stream()
                    .map(
                        feature ->
                            ProductCreateParams.MarketingFeature.builder().setName(feature).build())
                    .toList())
            .setActive(true)
            .putMetadata(
                VAT_PERCENT_METADATA_KEY,
                String.valueOf(vatPercentOrDefault(subscriptionProduct.getVatPercent())))
            .setDefaultPriceData(
                ProductCreateParams.DefaultPriceData.builder()
                    .setCurrency(defaultCurrency())
                    .setUnitAmount(subscriptionProduct.getPriceInCentsWithVat())
                    .setRecurring(
                        ProductCreateParams.DefaultPriceData.Recurring.builder()
                            .setInterval(
                                intervalFromSubscriptionType(subscriptionProduct.getType()))
                            .build())
                    .build());

    if (subscriptionProduct.getImageUrl() != null && !subscriptionProduct.getImageUrl().isBlank()) {
      productCreateParamsBuilder.addImage(subscriptionProduct.getImageUrl());
    }

    var createdStripeProduct = Product.create(productCreateParamsBuilder.build());
    log.info("createdStripeProductId: {}", createdStripeProduct.getId());
    return fromStripeProduct(randomUUID().toString(), createdStripeProduct);
  }

  private static long vatPercentOrDefault(Long vatPercent) {
    return vatPercent == null ? SubscriptionProduct.DEFAULT_VAT_PERCENT : vatPercent;
  }

  public void backfillStripeProductsVatMetadata() {
    var products = subscriptionProductRepository.findAll();
    log.info(
        "Stripe VAT metadata backfill starting for {} subscription product(s)", products.size());
    for (var product : products) {
      if (product.getE2Id() == null || product.getVatPercent() == null) {
        continue;
      }
      try {
        Product.retrieve(product.getE2Id())
            .update(
                ProductUpdateParams.builder()
                    .putMetadata(VAT_PERCENT_METADATA_KEY, String.valueOf(product.getVatPercent()))
                    .build());
        log.info(
            "Backfilled VAT metadata (vat_percent={}) on Stripe product {}",
            product.getVatPercent(),
            product.getE2Id());
      } catch (StripeException e) {
        log.error(
            "Failed to backfill VAT metadata on Stripe product {}, skipping", product.getE2Id(), e);
      }
    }
  }

  private static long vatPercentFromMetadata(Product stripeProduct) {
    var metadata = stripeProduct.getMetadata();
    var rawVatPercent = metadata == null ? null : metadata.get(VAT_PERCENT_METADATA_KEY);
    if (rawVatPercent == null) {
      return SubscriptionProduct.DEFAULT_VAT_PERCENT;
    }
    try {
      return Long.parseLong(rawVatPercent);
    } catch (NumberFormatException e) {
      log.warn(
          "Unparseable {} metadata '{}' on Stripe product {}, falling back to default VAT",
          VAT_PERCENT_METADATA_KEY,
          rawVatPercent,
          stripeProduct.getId());
      return SubscriptionProduct.DEFAULT_VAT_PERCENT;
    }
  }

  @SneakyThrows
  private SubscriptionProduct fromStripeProduct(
      String domainProductId, Product createdStripeProduct) {
    var createdDefaultPriceId = createdStripeProduct.getDefaultPrice();
    var price = Price.retrieve(createdDefaultPriceId);
    var vatPercent = vatPercentFromMetadata(createdStripeProduct);
    var subscriptionProductToPersistBuilder =
        SubscriptionProduct.builder()
            .id(domainProductId)
            .e2Id(createdStripeProduct.getId())
            .name(createdStripeProduct.getName())
            .description(createdStripeProduct.getDescription())
            .features(
                createdStripeProduct.getMarketingFeatures().stream()
                    .map(Product.MarketingFeature::getName)
                    .toList())
            .priceInCentsWithoutVat(
                SubscriptionProduct.priceInCentsWithoutVatFrom(price.getUnitAmount(), vatPercent))
            .vatPercent(vatPercent)
            .type(computeTypeFromRecurring(price.getRecurring().getInterval()))
            .creationDatetime(Instant.ofEpochSecond(createdStripeProduct.getCreated()));

    if (!createdStripeProduct.getImages().isEmpty()
        && createdStripeProduct.getImages().getFirst() != null
        && !createdStripeProduct.getImages().getFirst().isBlank()) {
      subscriptionProductToPersistBuilder.imageUrl(createdStripeProduct.getImages().getFirst());
    }

    subscriptionProductRepository
        .findById(domainProductId)
        .ifPresent(
            existing -> {
              subscriptionProductToPersistBuilder
                  .consumptionTypeAttached(existing.getConsumptionTypeAttached())
                  .planCode(existing.getPlanCode())
                  .billingType(existing.getBillingType())
                  .freeUsageThreshold(existing.getFreeUsageThreshold())
                  .includedCreditsPerBillingPeriod(existing.getIncludedCreditsPerBillingPeriod())
                  .creditUnitPriceInCentsWithoutVat(existing.getCreditUnitPriceInCentsWithoutVat())
                  .creditCostPerAnalysis(existing.getCreditCostPerAnalysis())
                  .overageUnitPriceInCents(existing.getOverageUnitPriceInCents())
                  .trialPeriodDays(existing.getTrialPeriodDays())
                  .annualDiscountPercent(existing.getAnnualDiscountPercent())
                  .annualE2PriceId(existing.getAnnualE2PriceId())
                  .annualPriceInCentsWithVat(existing.getAnnualPriceInCentsWithVat())
                  .meteredProductId(existing.getMeteredProductId())
                  .mostChosen(existing.isMostChosen())
                  .deprecated(existing.isDeprecated())
                  .displayPosition(existing.getDisplayPosition());
              if (createdStripeProduct.getMarketingFeatures() == null
                  || createdStripeProduct.getMarketingFeatures().isEmpty()) {
                subscriptionProductToPersistBuilder.features(existing.getFeatures());
              }
            });

    return subscriptionProductRepository.save(subscriptionProductToPersistBuilder.build());
  }

  private ProductCreateParams.DefaultPriceData.Recurring.Interval intervalFromSubscriptionType(
      SubscriptionType subscriptionType) {
    if (Objects.requireNonNull(subscriptionType) == SubscriptionType.MONTHLY) {
      return ProductCreateParams.DefaultPriceData.Recurring.Interval.MONTH;
    }
    throw new IllegalArgumentException("Unknown subscription type " + subscriptionType);
  }

  @SneakyThrows
  public Redirection initiateSubscription(
      User user, Subscription subscription, RedirectionStatusUrls redirectionUrls) {
    var unpaidStripeInvoices =
        stripeInvoiceService.getUnpaidStripeInvoices(user.getUserSubscriptionId());
    if (!unpaidStripeInvoices.isEmpty()) {
      var totalAmountDue =
          unpaidStripeInvoices.stream()
              .mapToDouble(invoice -> invoice.getAmountRemaining() / 100.0)
              .sum();
      throw new BadRequestException(
          "Unable to initiate new subscription as you still have unpaid invoices totaling amount : "
              + totalAmountDue
              + " €");
    }
    var stripeCustomer = stripeCustomerService.getCustomer(user);
    var actualUserSubscription = getSubscriptionByUser(user);
    var latestSubscription = actualUserSubscription.getLatestSubscription();
    if (latestSubscription != null
        && latestSubscription.isActive()
        && !(TRIALING).equals(latestSubscription.getStatus())
        && !(CANCELED).equals(latestSubscription.getStatus())
        && !hasPendingCancellationAfterFirstInvoice(user.getUserSubscriptionId())) {
      throw new BadRequestException(
          "User.id="
              + user.getId()
              + " has active subscription until "
              + latestSubscription.getEndDatetime());
    }
    long billingCycleAnchor = computeBillingCycleAnchor();
    log.info(
        "Schedule start date = {}",
        Instant.ofEpochSecond(billingCycleAnchor).atZone(ZoneId.of("Europe/Paris")).toLocalDate());

    return stripeFactory.initiateSubscriptionWorkflow(
        stripeCustomer, redirectionUrls, billingCycleAnchor, subscription);
  }

  private Long computeBillingCycleAnchor() {
    var today = temporalUtils.today();
    var firstFullBillingPeriodStart =
        today.getDayOfMonth() == 1 ? today : temporalUtils.startOfMonthAfter(today);
    return firstFullBillingPeriodStart.atStartOfDay(ZoneId.of("Europe/Paris")).toEpochSecond();
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
    var activeScheduledSubscriptions = getActiveSubscriptionSchedules(stripeCustomerId);
    log.info(
        "Active scheduled subscriptions: {}",
        activeScheduledSubscriptions.stream()
            .map(SubscriptionSchedule::getCreated)
            .map(Instant::ofEpochSecond)
            .toList());
    List<com.stripe.model.Subscription> stripeSubscriptions;
    try {
      stripeSubscriptions =
          stripeSubscriptionService.getStripeSubscriptionsFromStripeCustomerId(stripeCustomerId);
      log.info(
          "Stripe subscriptions for stripeCustomerId: {}",
          stripeSubscriptions.stream().map(com.stripe.model.Subscription::getId).toList());
    } catch (InvalidRequestException e) {
      var exceptionMessage = e.getMessage();
      if (exceptionMessage.contains("No such customer")) {
        log.info(exceptionMessage);
        return new ArrayList<>();
      }
      throw new RuntimeException(e);
    }
    var initialSubscription =
        new ArrayList<>(stripeSubscriptions.stream().map(this::mapToDomain).toList());
    if (!activeScheduledSubscriptions.isEmpty()
        && stripeSubscriptions.stream()
            .noneMatch(
                stripeSubscription -> stripeSubscription.getStatus().equalsIgnoreCase("active"))) {
      var firstScheduledSubscription = activeScheduledSubscriptions.getFirst();
      var scheduledStripeSubscriptionStartDate =
          firstScheduledSubscription.getPhases().getFirst().getStartDate();
      var domainSubscriptionStartDate =
          Instant.ofEpochSecond(firstScheduledSubscription.getCreated());
      var domainSubscriptionEndDate = Instant.ofEpochSecond(scheduledStripeSubscriptionStartDate);
      var scheduledSubscriptionStatus =
          isFlaggedForCancelAfterFirstInvoice(firstScheduledSubscription) ? CANCELED : ACTIVE;
      var subscriptionsFromSchedule =
          defaultActiveSubscription(
              scheduledSubscriptionStatus, domainSubscriptionStartDate, domainSubscriptionEndDate);
      initialSubscription.addAll(subscriptionsFromSchedule);
      log.info("Return subscriptions {}", initialSubscription);
      return initialSubscription;
    }
    log.info("Return subscriptions {}", initialSubscription);
    return initialSubscription;
  }

  private List<SubscriptionSchedule> getActiveSubscriptionSchedules(String stripeCustomerId)
      throws StripeException {
    List<SubscriptionSchedule> scheduledSubscriptions;
    try {
      scheduledSubscriptions =
          stripeClient
              .subscriptionSchedules()
              .list(SubscriptionScheduleListParams.builder().setCustomer(stripeCustomerId).build())
              .getData();
    } catch (InvalidRequestException e) {
      var exceptionMessage = e.getMessage();
      if (exceptionMessage.contains("No such customer")) {
        log.info(exceptionMessage);
        return new ArrayList<>();
      }
      throw new RuntimeException(e);
    }
    return scheduledSubscriptions.stream()
        .filter(
            subscriptionSchedule ->
                subscriptionSchedule.getCanceledAt() == null
                    && subscriptionSchedule.getStatus().equalsIgnoreCase("not_started"))
        .toList();
  }

  private boolean hasPendingCancellationAfterFirstInvoice(String stripeCustomerId)
      throws StripeException {
    if (stripeCustomerId == null) {
      return false;
    }
    var activeScheduledSubscriptions = getActiveSubscriptionSchedules(stripeCustomerId);
    return !activeScheduledSubscriptions.isEmpty()
        && activeScheduledSubscriptions.stream()
            .allMatch(SubscriptionService::isFlaggedForCancelAfterFirstInvoice);
  }

  private static boolean isFlaggedForCancelAfterFirstInvoice(SubscriptionSchedule schedule) {
    var metadata = schedule.getMetadata();
    return metadata != null && "true".equals(metadata.get(CANCEL_AFTER_FIRST_INVOICE_METADATA_KEY));
  }

  private Subscription mapToDomain(com.stripe.model.Subscription stripeSubscription) {
    var currentPeriodStartLongValue = stripeSubscription.getCurrentPeriodStart();
    var startDatetime =
        currentPeriodStartLongValue == null
            ? null
            : Instant.ofEpochSecond(currentPeriodStartLongValue);
    var currentPeriodEndLongValue = stripeSubscription.getCurrentPeriodEnd();
    var endDatetime =
        currentPeriodEndLongValue == null ? null : Instant.ofEpochSecond(currentPeriodEndLongValue);
    var status = computeUserSubscriptionStatus(stripeSubscription);
    var paymentSettings = stripeSubscription.getPaymentSettings();
    return Subscription.builder()
        .id(randomUUID().toString()) // TODO: update when subscription history persisted
        .e2Id(stripeSubscription.getId())
        .startDatetime(startDatetime)
        .endDatetime(endDatetime)
        .billingInterval(billingIntervalOf(stripeSubscription))
        .status(status)
        .active(!status.equals(UNKNOWN))
        .paymentMethods(
            paymentSettings == null ? new ArrayList<>() : paymentSettings.getPaymentMethodTypes())
        .build();
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
      case "past_due", "unpaid", "incomplete", "incomplete_expired" -> UNPAID;
      default -> {
        log.error("Unknown subscription status: {}", subscriptionStatus);
        yield UNKNOWN;
      }
    };
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
    return cancelLatestUserSubscription(user, null);
  }

  @SneakyThrows
  public UserSubscription cancelLatestUserSubscription(
      User user, SubscriptionCancellationType cancellationType) {
    if (user.getUserSubscriptionId() == null) {
      throw new IllegalArgumentException(
          "User.userSubscriptionId is required to cancel subscription, "
              + "otherwise User.id="
              + user.getId()
              + " does not have userSubscriptionId");
    }
    var effectiveCancellationType =
        cancellationType == null ? DEFAULT_CANCELLATION_TYPE : cancellationType;

    var activeScheduledSubscriptions = getActiveSubscriptionSchedules(user.getUserSubscriptionId());

    if (!activeScheduledSubscriptions.isEmpty() && effectiveCancellationType == END_OF_PERIOD) {
      var earliestScheduledStartDate = Long.MAX_VALUE;
      for (var scheduledSubscription : activeScheduledSubscriptions) {
        var scheduledStartDate = cancelScheduleAfterUpcomingPayment(scheduledSubscription);
        earliestScheduledStartDate = Math.min(earliestScheduledStartDate, scheduledStartDate);
      }

      userSubscriptionProductService.endActiveSubscriptionProducts(
          user.getId(), Instant.ofEpochSecond(earliestScheduledStartDate));

      var actualSubscriptions = getSubscriptionsFromStripeCustomer(user.getUserSubscriptionId());
      return UserSubscription.builder().user(user).subscriptions(actualSubscriptions).build();
    }

    for (var scheduledSubscription : activeScheduledSubscriptions) {
      cancelScheduleImmediately(scheduledSubscription);
    }

    var subscriptions = getSubscriptionsFromStripeCustomer(user.getUserSubscriptionId());
    var cancellableStripeSubscriptionIds =
        stripeSubscriptionService
            .getStripeSubscriptionsFromStripeCustomerId(user.getUserSubscriptionId())
            .stream()
            .filter(
                stripeSubscription -> !StripeSubscriptionService.isTerminated(stripeSubscription))
            .map(com.stripe.model.Subscription::getId)
            .collect(Collectors.toSet());
    var activeSubscriptions =
        subscriptions.stream()
            .filter(Subscription::isActive)
            .filter(subscription -> subscription.getE2Id() != null)
            .filter(
                subscription -> cancellableStripeSubscriptionIds.contains(subscription.getE2Id()))
            .toList();
    if (activeSubscriptions.isEmpty() && !activeScheduledSubscriptions.isEmpty()) {
      userSubscriptionProductService.endActiveSubscriptionProducts(user.getId(), now());

      var actualSubscriptions = getSubscriptionsFromStripeCustomer(user.getUserSubscriptionId());
      return UserSubscription.builder().user(user).subscriptions(actualSubscriptions).build();
    }
    if (subscriptions.isEmpty()) {
      throw new BadRequestException("User.id=" + user.getId() + " does not have any subscriptions");
    }
    if (activeSubscriptions.isEmpty()) {
      throw new IllegalStateException(
          "Only active subscription can be cancelled but none of the "
              + subscriptions.size()
              + " subscription(s) is active");
    }
    for (var activeSubscription : activeSubscriptions) {
      if (effectiveCancellationType == END_OF_PERIOD) {
        stripeClient
            .subscriptions()
            .update(
                activeSubscription.getE2Id(),
                SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.NONE)
                    .build());
      } else {
        stripeClient
            .subscriptions()
            .cancel(
                activeSubscription.getE2Id(),
                SubscriptionCancelParams.builder().setProrate(false).setInvoiceNow(false).build());
      }
    }
    var latestSubscription =
        activeSubscriptions.stream()
            .sorted(comparing(Subscription::getStartDatetime, nullsLast(naturalOrder())).reversed())
            .toList()
            .getFirst();
    var periodEnd =
        effectiveCancellationType == END_OF_PERIOD && latestSubscription.getEndDatetime() != null
            ? latestSubscription.getEndDatetime()
            : now();

    userSubscriptionProductService.endActiveSubscriptionProducts(user.getId(), periodEnd);

    var actualSubscriptions = getSubscriptionsFromStripeCustomer(user.getUserSubscriptionId());
    return UserSubscription.builder().user(user).subscriptions(actualSubscriptions).build();
  }

  private void cancelScheduleImmediately(SubscriptionSchedule scheduledSubscription)
      throws StripeException {
    stripeClient
        .subscriptionSchedules()
        .cancel(
            scheduledSubscription.getId(),
            SubscriptionScheduleCancelParams.builder()
                .setProrate(false)
                .setInvoiceNow(false)
                .build());
    log.info("Cancelled scheduled subscription {} immediately", scheduledSubscription.getId());
  }

  private long cancelScheduleAfterUpcomingPayment(SubscriptionSchedule scheduledSubscription)
      throws StripeException {
    var upcomingPhase = scheduledSubscription.getPhases().getFirst();
    var scheduledStartDate = upcomingPhase.getStartDate();
    var phaseItems =
        upcomingPhase.getItems().stream()
            .map(
                item ->
                    SubscriptionScheduleUpdateParams.Phase.Item.builder()
                        .setPrice(item.getPrice())
                        .build())
            .toList();
    scheduledSubscription.update(
        SubscriptionScheduleUpdateParams.builder()
            .setEndBehavior(SubscriptionScheduleUpdateParams.EndBehavior.CANCEL)
            .addPhase(
                SubscriptionScheduleUpdateParams.Phase.builder()
                    .addAllItem(phaseItems)
                    .setStartDate(scheduledStartDate)
                    .setIterations(1L)
                    .build())
            .putMetadata(CANCEL_AFTER_FIRST_INVOICE_METADATA_KEY, "true")
            .setProrationBehavior(SubscriptionScheduleUpdateParams.ProrationBehavior.NONE)
            .build());
    return scheduledStartDate;
  }

  @SneakyThrows
  public void cancelScheduledSubscriptionAfterInvoicePaid(String stripeSubscriptionId) {
    if (stripeSubscriptionId == null) {
      return;
    }
    var subscription = stripeClient.subscriptions().retrieve(stripeSubscriptionId);
    var scheduleId = subscription.getSchedule();
    if (scheduleId == null) {
      return;
    }
    var schedule = stripeClient.subscriptionSchedules().retrieve(scheduleId);
    if (!isFlaggedForCancelAfterFirstInvoice(schedule)) {
      return;
    }
    if ("canceled".equalsIgnoreCase(schedule.getStatus())) {
      log.info("Scheduled subscription {} already canceled, skipping", scheduleId);
      return;
    }
    stripeClient
        .subscriptionSchedules()
        .cancel(
            scheduleId,
            SubscriptionScheduleCancelParams.builder()
                .setProrate(false)
                .setInvoiceNow(false)
                .build());
    log.info(
        "Cancelled scheduled subscription {} immediately after its first invoice was paid",
        scheduleId);
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
      return SubscriptionType.MONTHLY;
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
    return fromStripeProduct(domainProductId, Product.retrieve(e2Id));
  }

  /**
   * Resolves the actually-subscribed plan, and the interval it is billed on, from a Stripe
   * subscription. A subscription carries several items (the base plan plus the metered/usage
   * product); only the subscribable plan (billingType != null) is returned so the metered product
   * is never mistaken for the plan.
   */
  public Optional<SubscribedPlan> resolveSubscribedPlan(
      com.stripe.model.Subscription stripeSubscription) {
    if (stripeSubscription.getItems() == null) {
      return Optional.empty();
    }
    return stripeSubscription.getItems().getData().stream()
        .map(SubscriptionItem::getPrice)
        .filter(Objects::nonNull)
        .map(this::subscribedPlanFrom)
        .flatMap(Optional::stream)
        .findFirst();
  }

  /**
   * Same as {@link #resolveSubscribedPlan(com.stripe.model.Subscription)} but for a scheduled
   * subscription, whose phase items only reference price ids and therefore require the prices to be
   * retrieved to reach their products.
   */
  @SneakyThrows
  public Optional<SubscribedPlan> resolveSubscribedPlan(SubscriptionSchedule schedule) {
    if (schedule.getPhases() == null || schedule.getPhases().isEmpty()) {
      return Optional.empty();
    }
    var priceIds =
        schedule.getPhases().stream()
            .flatMap(phase -> phase.getItems().stream())
            .map(SubscriptionSchedule.Phase.Item::getPrice)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    for (var priceId : priceIds) {
      var subscribedPlan = subscribedPlanFrom(stripeClient.prices().retrieve(priceId));
      if (subscribedPlan.isPresent()) {
        return subscribedPlan;
      }
    }
    return Optional.empty();
  }

  private Optional<SubscribedPlan> subscribedPlanFrom(Price price) {
    var productE2Id = price.getProduct();
    if (productE2Id == null) {
      return Optional.empty();
    }
    return subscriptionProductRepository
        .findByE2Id(productE2Id)
        .filter(product -> product.getBillingType() != null)
        .map(product -> new SubscribedPlan(product.getId(), billingIntervalOf(price, product)));
  }

  private static BillingInterval billingIntervalOf(Price price, SubscriptionProduct plan) {
    if (price.getId() != null && price.getId().equals(plan.getAnnualE2PriceId())) {
      return BillingInterval.YEARLY;
    }
    return billingIntervalOf(price);
  }

  private static BillingInterval billingIntervalOf(Price price) {
    var recurring = price.getRecurring();
    return recurring != null && STRIPE_YEARLY_INTERVAL.equals(recurring.getInterval())
        ? BillingInterval.YEARLY
        : BillingInterval.MONTHLY;
  }

  private static BillingInterval billingIntervalOf(
      com.stripe.model.Subscription stripeSubscription) {
    if (stripeSubscription.getItems() == null) {
      return BillingInterval.MONTHLY;
    }
    return stripeSubscription.getItems().getData().stream()
            .map(SubscriptionItem::getPrice)
            .filter(Objects::nonNull)
            .map(SubscriptionService::billingIntervalOf)
            .anyMatch(BillingInterval.YEARLY::equals)
        ? BillingInterval.YEARLY
        : BillingInterval.MONTHLY;
  }

  public record SubscribedPlan(String planId, BillingInterval billingInterval) {}
}
