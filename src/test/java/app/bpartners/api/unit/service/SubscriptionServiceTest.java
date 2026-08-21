package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.SubscriptionCancellationType.END_OF_PERIOD;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionType;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.*;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import app.bpartners.api.repository.jpa.*;
import app.bpartners.api.repository.jpa.model.detection.HDetectionTracking;
import app.bpartners.api.service.subscription.*;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import com.stripe.service.CustomerService;
import com.stripe.service.SubscriptionScheduleService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;

class SubscriptionServiceTest {
  private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceTest.class);
  StripeConf stripeConfMock = mock(StripeConf.class);
  StripeClient stripeClientMock = mock(StripeClient.class);
  UserRepository userRepositoryMock = mock(UserRepository.class);
  SubscriptionProductRepository subscriptionProductRepositoryMock =
      mock(SubscriptionProductRepository.class);
  UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepositoryMock =
      mock(UserSubscriptionEligibleJpaRepository.class);
  SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepositoryMock =
      mock(SubscriptionConsumptionLogJpaRepository.class);
  TemporalUtils temporalUtils = new TemporalUtils();
  StripeFactory sessionFactoryMock = mock(StripeFactory.class);
  DetectionTrackingJpaRepository detectionTrackingJpaRepositoryMock =
      mock(DetectionTrackingJpaRepository.class);
  StripeInvoiceService stripeInvoiceServiceMock = mock();
  StripeCustomerService stripeCustomerServiceMock = mock(StripeCustomerService.class);
  StripeSubscriptionService stripeSubscriptionServiceMock = mock();
  UserSubscriptionProductService userSubscriptionProductServiceMock =
      mock(UserSubscriptionProductService.class);
  UserSubscriptionCommitmentJpaRepository userSubscriptionCommitmentJpaRepositoryMock = mock();
  UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository
      userSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepositoryMock = mock();
  SubscriptionService subject =
      new SubscriptionService(
          stripeConfMock,
          stripeClientMock,
          userRepositoryMock,
          subscriptionProductRepositoryMock,
          subscriptionEligibleJpaRepositoryMock,
          temporalUtils,
          consumptionLogJpaRepositoryMock,
          sessionFactoryMock,
          detectionTrackingJpaRepositoryMock,
          stripeInvoiceServiceMock,
          stripeCustomerServiceMock,
          stripeSubscriptionServiceMock,
          userSubscriptionProductServiceMock,
          userSubscriptionCommitmentJpaRepositoryMock,
          userSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepositoryMock);

  @Test
  void get_subscription_consumption_logs_ok() {
    var userId = randomUUID().toString();
    var startOfMonth = temporalUtils.startOfMonth();
    var endOfMonth = temporalUtils.endOfMonth();

    var expectedDetectionTrackingEntities = List.of(someDetectionTracking(userId, now()));
    var expected =
        expectedDetectionTrackingEntities.stream()
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
    when(detectionTrackingJpaRepositoryMock.findAllByIdUserAndCreationDatetimeBetween(
            userId, startOfMonth, endOfMonth))
        .thenReturn(expectedDetectionTrackingEntities);

    var actualWithOverrideFilterValues =
        subject.findConsumptionLogsByUserId(userId, startOfMonth, endOfMonth);

    var actualWithNullFilterValues = subject.findConsumptionLogsByUserId(userId, null, null);

    assertEquals(expected, actualWithOverrideFilterValues);
    assertEquals(expected, actualWithNullFilterValues);
  }

  private static SubscriptionConsumptionLog someConsumptionLog(
      String userId, Instant creationDatetime) {
    return SubscriptionConsumptionLog.builder()
        .id(randomUUID().toString())
        .userId(userId)
        .consumptionType(ROOF_ANALYSIS)
        .usageMetric(1L)
        .consumptionUnit(UNIT)
        .creationDatetime(creationDatetime)
        .build();
  }

  private static HDetectionTracking someDetectionTracking(String userId, Instant creationDatetime) {
    return HDetectionTracking.builder()
        .id(randomUUID().toString())
        .idUser(userId)
        .creationDatetime(creationDatetime)
        .build();
  }

  @Test
  void get_by_subscription_type_ok() {
    try (MockedStatic<Product> productMockedStatic = mockStatic(Product.class);
        MockedStatic<Price> priceMockedStatic = mockStatic(Price.class)) {

      var userSubscriptionType = UserSubscriptionType.ESSENTIAL;
      when(stripeConfMock.getEssentialSubscriptionProductId())
          .thenReturn("esentialSubscriptionProductId");
      var subscriptionProduct =
          SubscriptionProduct.builder()
              .id("subscriptionProductId")
              .e2Id("stripeProductId")
              .meteredProductId("roofAnalysis")
              .build();
      when(subscriptionProductRepositoryMock.findById(any()))
          .thenReturn(Optional.of(subscriptionProduct));
      var product = new Product();
      product.setDefaultPrice("");
      product.setMarketingFeatures(List.of(new Product.MarketingFeature()));
      product.setImages(List.of("image"));
      product.setCreated(1L);
      var price = new Price();
      var recurring = new Price.Recurring();
      price.setRecurring(recurring);
      recurring.setInterval("month");
      when(subscriptionProductRepositoryMock.save(any()))
          .thenReturn(
              subscriptionProduct.toBuilder()
                  .e2Id(product.getId())
                  .name(product.getName())
                  .description(product.getDescription())
                  .features(
                      product.getMarketingFeatures().stream()
                          .map(Product.MarketingFeature::getName)
                          .toList())
                  .priceInCentsWithoutVat(price.getUnitAmount())
                  .imageUrl(product.getImages().getFirst())
                  .type(MONTHLY)
                  .creationDatetime(now())
                  .build());

      productMockedStatic.when(() -> Product.retrieve(any())).thenReturn(product);
      priceMockedStatic.when(() -> Price.retrieve(any())).thenReturn(price);
      var actual = subject.getBySubscriptionType(userSubscriptionType);
      log.info("actual: {}", actual);

      var subscriptionProductExpected =
          SubscriptionProduct.builder()
              .id(actual.getSubscriptionProduct().getId())
              .e2Id(product.getId())
              .name(product.getName())
              .description(product.getDescription())
              .features(
                  product.getMarketingFeatures().stream()
                      .map(Product.MarketingFeature::getName)
                      .toList())
              .priceInCentsWithoutVat(price.getUnitAmount())
              .imageUrl(product.getImages().getFirst())
              .type(MONTHLY)
              .meteredProductId("roofAnalysis")
              .creationDatetime(actual.getSubscriptionProduct().getCreationDatetime())
              .build();
      var expected =
          Subscription.builder()
              .subscriptionProduct(subscriptionProductExpected)
              .endDatetime(actual.getEndDatetime())
              .build();
      assertEquals(expected, actual);
    }
  }

  @Test
  void get_by_plan_id_yearly_without_annual_pricing_ko() {
    var planId = "planId";
    var product = SubscriptionProduct.builder().id(planId).e2Id("stripeProductId").build();
    when(subscriptionProductRepositoryMock.findById(planId)).thenReturn(Optional.of(product));

    assertThrows(
        BadRequestException.class, () -> subject.getByPlanId(planId, BillingInterval.YEARLY));
  }

  @Test
  void get_by_plan_id_yearly_ok_sets_billing_interval() {
    try (MockedStatic<Product> productMockedStatic = mockStatic(Product.class);
        MockedStatic<Price> priceMockedStatic = mockStatic(Price.class)) {
      var planId = "planId";
      var product =
          SubscriptionProduct.builder()
              .id(planId)
              .e2Id("stripeProductId")
              .vatPercent(2000L)
              .annualE2PriceId("annual_price_id")
              .annualPriceInCentsWithVat(63504L)
              .meteredProductId("roofAnalysis")
              .build();
      when(subscriptionProductRepositoryMock.findById(planId)).thenReturn(Optional.of(product));
      var stripeProduct = new Product();
      stripeProduct.setDefaultPrice("priceId");
      stripeProduct.setMarketingFeatures(List.of());
      stripeProduct.setImages(List.of());
      stripeProduct.setCreated(1L);
      var stripePrice = new Price();
      var recurring = new Price.Recurring();
      recurring.setInterval("month");
      stripePrice.setRecurring(recurring);
      stripePrice.setUnitAmount(5880L);
      productMockedStatic.when(() -> Product.retrieve(any())).thenReturn(stripeProduct);
      priceMockedStatic.when(() -> Price.retrieve(any())).thenReturn(stripePrice);
      when(subscriptionProductRepositoryMock.save(any()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      var actual = subject.getByPlanId(planId, BillingInterval.YEARLY);

      assertEquals(BillingInterval.YEARLY, actual.getBillingInterval());
    }
  }

  @Test
  void get_subscription_product_by_e2id_preserves_annual_columns_when_remirroring() {
    try (MockedStatic<Product> productMockedStatic = mockStatic(Product.class);
        MockedStatic<Price> priceMockedStatic = mockStatic(Price.class)) {
      var domainProductId = "planId";
      var existing =
          SubscriptionProduct.builder()
              .id(domainProductId)
              .e2Id("stripeProductId")
              .vatPercent(2000L)
              .annualE2PriceId("annual_price_id")
              .annualPriceInCentsWithVat(63504L)
              .build();
      when(subscriptionProductRepositoryMock.findById(domainProductId))
          .thenReturn(Optional.of(existing));
      var product = new Product();
      product.setDefaultPrice("priceId");
      product.setMarketingFeatures(List.of());
      product.setImages(List.of());
      product.setCreated(1L);
      var price = new Price();
      var recurring = new Price.Recurring();
      recurring.setInterval("month");
      price.setRecurring(recurring);
      price.setUnitAmount(5880L);
      productMockedStatic.when(() -> Product.retrieve(any())).thenReturn(product);
      priceMockedStatic.when(() -> Price.retrieve(any())).thenReturn(price);
      when(subscriptionProductRepositoryMock.save(any()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      subject.getSubscriptionProductByE2Id(domainProductId, "stripeProductId");

      var captor = ArgumentCaptor.forClass(SubscriptionProduct.class);
      verify(subscriptionProductRepositoryMock).save(captor.capture());
      var saved = captor.getValue();
      assertEquals("annual_price_id", saved.getAnnualE2PriceId());
      assertEquals(Long.valueOf(63504L), saved.getAnnualPriceInCentsWithVat());
    }
  }

  @Test
  void get_subscription_product_by_e2id_preserves_catalog_columns_when_remirroring() {
    try (MockedStatic<Product> productMockedStatic = mockStatic(Product.class);
        MockedStatic<Price> priceMockedStatic = mockStatic(Price.class)) {
      var domainProductId = "planId";
      var existing =
          SubscriptionProduct.builder()
              .id(domainProductId)
              .e2Id("stripeProductId")
              .planCode("ESSENTIAL")
              .billingType(SubscriptionBillingType.COMMITMENT)
              .freeUsageThreshold(20L)
              .vatPercent(2000L)
              .overageUnitPriceInCents(200L)
              .trialPeriodDays(7)
              .features(List.of("feature-a", "feature-b"))
              .mostChosen(true)
              .deprecated(true)
              .displayPosition(3)
              .build();
      when(subscriptionProductRepositoryMock.findById(domainProductId))
          .thenReturn(Optional.of(existing));
      var product = new Product();
      product.setDefaultPrice("priceId");
      product.setMarketingFeatures(List.of());
      product.setImages(List.of());
      product.setCreated(1L);
      var price = new Price();
      var recurring = new Price.Recurring();
      recurring.setInterval("month");
      price.setRecurring(recurring);
      price.setUnitAmount(5880L);
      productMockedStatic.when(() -> Product.retrieve(any())).thenReturn(product);
      priceMockedStatic.when(() -> Price.retrieve(any())).thenReturn(price);
      when(subscriptionProductRepositoryMock.save(any()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      subject.getSubscriptionProductByE2Id(domainProductId, "stripeProductId");

      var captor = ArgumentCaptor.forClass(SubscriptionProduct.class);
      verify(subscriptionProductRepositoryMock).save(captor.capture());
      var saved = captor.getValue();
      assertEquals("ESSENTIAL", saved.getPlanCode());
      assertEquals(SubscriptionBillingType.COMMITMENT, saved.getBillingType());
      assertEquals(Long.valueOf(20L), saved.getFreeUsageThreshold());
      assertEquals(Long.valueOf(200L), saved.getOverageUnitPriceInCents());
      assertEquals(Integer.valueOf(7), saved.getTrialPeriodDays());
      assertEquals(Long.valueOf(5880L), saved.getPriceInCentsWithVat());
      // Catalog-only columns must survive a re-mirror even though Stripe does not carry them.
      assertTrue(saved.isMostChosen());
      assertTrue(saved.isDeprecated());
      assertEquals(Integer.valueOf(3), saved.getDisplayPosition());
      // Stripe has no marketing features here, so the catalog-defined features must be kept.
      assertEquals(List.of("feature-a", "feature-b"), saved.getFeatures());
    }
  }

  @Test
  void get_subscription_product_by_e2id_preserves_credit_columns_when_remirroring() {
    try (MockedStatic<Product> productMockedStatic = mockStatic(Product.class);
        MockedStatic<Price> priceMockedStatic = mockStatic(Price.class)) {
      var domainProductId = "planId";
      var existing =
          SubscriptionProduct.builder()
              .id(domainProductId)
              .e2Id("stripeProductId")
              .vatPercent(2000L)
              .creditUnitPriceInCentsWithoutVat(1500L)
              .creditCostPerAnalysis(3L)
              .includedCreditsPerBillingPeriod(50L)
              .build();
      when(subscriptionProductRepositoryMock.findById(domainProductId))
          .thenReturn(Optional.of(existing));
      var product = new Product();
      product.setDefaultPrice("priceId");
      product.setMarketingFeatures(List.of());
      product.setImages(List.of());
      product.setCreated(1L);
      var price = new Price();
      var recurring = new Price.Recurring();
      recurring.setInterval("month");
      price.setRecurring(recurring);
      price.setUnitAmount(5880L);
      productMockedStatic.when(() -> Product.retrieve(any())).thenReturn(product);
      priceMockedStatic.when(() -> Price.retrieve(any())).thenReturn(price);
      when(subscriptionProductRepositoryMock.save(any()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      subject.getSubscriptionProductByE2Id(domainProductId, "stripeProductId");

      var captor = ArgumentCaptor.forClass(SubscriptionProduct.class);
      verify(subscriptionProductRepositoryMock).save(captor.capture());
      var saved = captor.getValue();
      assertEquals(Long.valueOf(1500L), saved.getCreditUnitPriceInCentsWithoutVat());
      assertEquals(Long.valueOf(3L), saved.getCreditCostPerAnalysis());
      assertEquals(Long.valueOf(50L), saved.getIncludedCreditsPerBillingPeriod());
    }
  }

  @Test
  void get_subscription_product_by_e2id_reads_vat_from_stripe_metadata() {
    try (MockedStatic<Product> productMockedStatic = mockStatic(Product.class);
        MockedStatic<Price> priceMockedStatic = mockStatic(Price.class)) {
      var domainProductId = "planId";
      when(subscriptionProductRepositoryMock.findById(domainProductId))
          .thenReturn(Optional.empty());
      var product = new Product();
      product.setDefaultPrice("priceId");
      product.setMarketingFeatures(List.of());
      product.setImages(List.of());
      product.setCreated(1L);
      // VAT mirrored in the Stripe product metadata (5.5% here, not the default 20%).
      product.setMetadata(Map.of("vat_percent", "550"));
      var price = new Price();
      var recurring = new Price.Recurring();
      recurring.setInterval("month");
      price.setRecurring(recurring);
      price.setUnitAmount(1055L); // TTC
      productMockedStatic.when(() -> Product.retrieve(any())).thenReturn(product);
      priceMockedStatic.when(() -> Price.retrieve(any())).thenReturn(price);
      when(subscriptionProductRepositoryMock.save(any()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      subject.getSubscriptionProductByE2Id(domainProductId, "stripeProductId");

      var captor = ArgumentCaptor.forClass(SubscriptionProduct.class);
      verify(subscriptionProductRepositoryMock).save(captor.capture());
      var saved = captor.getValue();
      // VAT rate and HT price come from the metadata rate, not the hard-coded default.
      assertEquals(Long.valueOf(550L), saved.getVatPercent());
      assertEquals(Long.valueOf(1000L), saved.getPriceInCentsWithoutVat());
      assertEquals(Long.valueOf(1055L), saved.getPriceInCentsWithVat());
    }
  }

  @Test
  void backfill_stripe_vat_metadata_updates_only_products_with_e2id_and_vat() throws Exception {
    try (MockedStatic<Product> productMockedStatic = mockStatic(Product.class)) {
      var withVat =
          SubscriptionProduct.builder().id("p1").e2Id("stripe1").vatPercent(2000L).build();
      var withoutE2Id = SubscriptionProduct.builder().id("p2").vatPercent(550L).build();
      var withoutVat = SubscriptionProduct.builder().id("p3").e2Id("stripe3").build();
      when(subscriptionProductRepositoryMock.findAll())
          .thenReturn(List.of(withVat, withoutE2Id, withoutVat));
      var stripeProduct = mock(Product.class);
      productMockedStatic.when(() -> Product.retrieve("stripe1")).thenReturn(stripeProduct);

      subject.backfillStripeProductsVatMetadata();

      // Only the product that has both a Stripe id and a VAT rate is pushed to Stripe.
      productMockedStatic.verify(() -> Product.retrieve("stripe1"));
      productMockedStatic.verify(() -> Product.retrieve("stripe3"), never());
      var captor = ArgumentCaptor.forClass(ProductUpdateParams.class);
      verify(stripeProduct).update(captor.capture());
      @SuppressWarnings("unchecked")
      var metadata = (Map<String, String>) captor.getValue().getMetadata();
      assertEquals("2000", metadata.get("vat_percent"));
    }
  }

  @Test
  void get_subscribable_plans_applies_default_pagination_page1_size100() {
    var plan = SubscriptionProduct.builder().id("essential").build();
    var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    when(subscriptionProductRepositoryMock.findAllByBillingTypeNotNull(any(Pageable.class)))
        .thenReturn(List.of(plan));

    var actual = subject.getSubscribablePlans(null, null);

    assertEquals(List.of(plan), actual);
    verify(subscriptionProductRepositoryMock).findAllByBillingTypeNotNull(pageableCaptor.capture());
    var pageable = pageableCaptor.getValue();
    assertEquals(0, pageable.getPageNumber()); // page=1 -> 0-indexed offset
    assertEquals(100, pageable.getPageSize());
  }

  @Test
  void get_subscribable_plans_maps_page_from_one_to_zero_indexed() {
    var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    when(subscriptionProductRepositoryMock.findAllByBillingTypeNotNull(any(Pageable.class)))
        .thenReturn(List.of());

    subject.getSubscribablePlans(new PageFromOne(3), new BoundedPageSize(10));

    verify(subscriptionProductRepositoryMock).findAllByBillingTypeNotNull(pageableCaptor.capture());
    var pageable = pageableCaptor.getValue();
    assertEquals(2, pageable.getPageNumber());
    assertEquals(10, pageable.getPageSize());
  }

  @SneakyThrows
  @Test
  void cancel_subscription_ko() {
    var stripeCustomerWithEmptySubscriptionId = "stripeCustomerWithEmptySubscriptionId";
    var stripeSubscriptionServiceMock1 = mock(com.stripe.service.SubscriptionService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of());
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock1);
    var stripeCollectionMock = mock(StripeCollection.class);
    when(stripeSubscriptionServiceMock1.list(any(SubscriptionListParams.class)))
        .thenReturn(stripeCollectionMock);
    when(stripeCollectionMock.getData()).thenReturn(List.of());

    var actualEmptySubscriptionIdException =
        assertThrows(
            IllegalArgumentException.class, () -> subject.cancelLatestUserSubscription(new User()));
    var actualEmptySubscriptionException =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.cancelLatestUserSubscription(
                    User.builder()
                        .userSubscriptionId(stripeCustomerWithEmptySubscriptionId)
                        .build()));

    assertEquals(
        "User.userSubscriptionId is required to cancel subscription, otherwise User.id=null does"
            + " not have userSubscriptionId",
        actualEmptySubscriptionIdException.getMessage());
    assertEquals(
        "User.id=null does not have any subscriptions",
        actualEmptySubscriptionException.getMessage());
  }

  @SneakyThrows
  @Test
  void cancel_scheduled_subscription_at_period_end_ok() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();

    var scheduledStartEpoch = now().plus(1L, DAYS).getEpochSecond();
    var schedule = someNotStartedSchedule("schedule_id", scheduledStartEpoch, "price_base");
    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of(schedule));
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    // A running active subscription also exists so the schedule is not mirrored as synthetic.
    var activeStripeSubscription = new com.stripe.model.Subscription();
    activeStripeSubscription.setId("sub_active");
    activeStripeSubscription.setStatus("active");
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of(activeStripeSubscription));

    var actual = subject.cancelLatestUserSubscription(user, END_OF_PERIOD);

    // The schedule is not cancelled right away: it is updated to cancel after one billing cycle so
    // the upcoming due payment is still collected.
    verify(schedule, never()).cancel();
    var updateCaptor = ArgumentCaptor.forClass(SubscriptionScheduleUpdateParams.class);
    verify(schedule).update(updateCaptor.capture());
    var updateParams = updateCaptor.getValue();
    assertEquals(
        SubscriptionScheduleUpdateParams.EndBehavior.CANCEL, updateParams.getEndBehavior());
    var updatedPhase = updateParams.getPhases().getFirst();
    assertEquals(1L, updatedPhase.getIterations());
    assertEquals(scheduledStartEpoch, updatedPhase.getStartDate());
    @SuppressWarnings("unchecked")
    var updateMetadata = (Map<String, String>) updateParams.getMetadata();
    assertEquals("true", updateMetadata.get("cancel_after_first_invoice"));
    // Nothing is refunded when the pending phase is trimmed to a single iteration.
    assertEquals(
        SubscriptionScheduleUpdateParams.ProrationBehavior.NONE,
        updateParams.getProrationBehavior());
    verify(userSubscriptionProductServiceMock)
        .endActiveSubscriptionProducts("user_id", Instant.ofEpochSecond(scheduledStartEpoch));
    verify(stripeClientMock, never()).subscriptions();
    assertNotNull(actual);
  }

  private static SubscriptionSchedule someNotStartedSchedule(
      String scheduleId, long startDateEpoch, String basePriceId) {
    var item = mock(SubscriptionSchedule.Phase.Item.class);
    when(item.getPrice()).thenReturn(basePriceId);
    var phase = mock(SubscriptionSchedule.Phase.class);
    when(phase.getStartDate()).thenReturn(startDateEpoch);
    when(phase.getItems()).thenReturn(List.of(item));
    var schedule = mock(SubscriptionSchedule.class);
    when(schedule.getId()).thenReturn(scheduleId);
    when(schedule.getStatus()).thenReturn("not_started");
    when(schedule.getCanceledAt()).thenReturn(null);
    when(schedule.getPhases()).thenReturn(List.of(phase));
    return schedule;
  }

  @SneakyThrows
  @Test
  void cancel_current_subscription_at_period_end_ok() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();

    // No scheduled subscription -> the current running subscription is the one to cancel.
    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of());
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    var periodEndEpoch = now().plusSeconds(3600).getEpochSecond();
    var activeStripeSubscription = new com.stripe.model.Subscription();
    activeStripeSubscription.setId("sub_active");
    activeStripeSubscription.setStatus("active");
    activeStripeSubscription.setCurrentPeriodStart(now().getEpochSecond());
    activeStripeSubscription.setCurrentPeriodEnd(periodEndEpoch);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of(activeStripeSubscription));

    var subscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    when(stripeClientMock.subscriptions()).thenReturn(subscriptionServiceMock);

    var actual = subject.cancelLatestUserSubscription(user, END_OF_PERIOD);

    var idCaptor = ArgumentCaptor.forClass(String.class);
    var paramsCaptor = ArgumentCaptor.forClass(SubscriptionUpdateParams.class);
    verify(subscriptionServiceMock).update(idCaptor.capture(), paramsCaptor.capture());
    assertEquals("sub_active", idCaptor.getValue());
    assertEquals(true, paramsCaptor.getValue().getCancelAtPeriodEnd());
    // Nothing is refunded: the already paid period is kept as is.
    assertEquals(
        SubscriptionUpdateParams.ProrationBehavior.NONE,
        paramsCaptor.getValue().getProrationBehavior());
    // The active product is ended at the current period end, not at cancel time.
    verify(userSubscriptionProductServiceMock)
        .endActiveSubscriptionProducts("user_id", Instant.ofEpochSecond(periodEndEpoch));
    assertNotNull(actual);
  }

  @SneakyThrows
  @Test
  void cancel_current_subscription_immediately_by_default() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();

    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of());
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    var periodEndEpoch = now().plusSeconds(3600).getEpochSecond();
    var activeStripeSubscription = new com.stripe.model.Subscription();
    activeStripeSubscription.setId("sub_active");
    activeStripeSubscription.setStatus("active");
    activeStripeSubscription.setCurrentPeriodStart(now().getEpochSecond());
    activeStripeSubscription.setCurrentPeriodEnd(periodEndEpoch);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of(activeStripeSubscription));

    var subscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    when(stripeClientMock.subscriptions()).thenReturn(subscriptionServiceMock);

    var actual = subject.cancelLatestUserSubscription(user);

    var idCaptor = ArgumentCaptor.forClass(String.class);
    var paramsCaptor = ArgumentCaptor.forClass(SubscriptionCancelParams.class);
    verify(subscriptionServiceMock).cancel(idCaptor.capture(), paramsCaptor.capture());
    assertEquals("sub_active", idCaptor.getValue());
    assertEquals(false, paramsCaptor.getValue().getProrate());
    assertEquals(false, paramsCaptor.getValue().getInvoiceNow());
    verify(subscriptionServiceMock, never())
        .update(anyString(), any(SubscriptionUpdateParams.class));
    var endDatetimeCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(userSubscriptionProductServiceMock)
        .endActiveSubscriptionProducts(eq("user_id"), endDatetimeCaptor.capture());
    // The access stops right away instead of lasting until the current period end.
    assertTrue(endDatetimeCaptor.getValue().isBefore(Instant.ofEpochSecond(periodEndEpoch)));
    assertNotNull(actual);
  }

  @SneakyThrows
  @Test
  void cancel_scheduled_subscription_immediately_by_default() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();

    var scheduledStartEpoch = now().plus(1L, DAYS).getEpochSecond();
    var schedule = someNotStartedSchedule("schedule_id", scheduledStartEpoch, "price_base");
    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of(schedule));
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    var activeStripeSubscription = new com.stripe.model.Subscription();
    activeStripeSubscription.setId("sub_active");
    activeStripeSubscription.setStatus("active");
    activeStripeSubscription.setCurrentPeriodStart(now().getEpochSecond());
    activeStripeSubscription.setCurrentPeriodEnd(now().plusSeconds(3600).getEpochSecond());
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of(activeStripeSubscription));

    var subscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    when(stripeClientMock.subscriptions()).thenReturn(subscriptionServiceMock);

    var actual = subject.cancelLatestUserSubscription(user);

    // The upcoming phase is dropped instead of being billed one last time.
    verify(schedule, never()).update(any(SubscriptionScheduleUpdateParams.class));
    var scheduleCancelCaptor = ArgumentCaptor.forClass(SubscriptionScheduleCancelParams.class);
    verify(subscriptionScheduleServiceMock)
        .cancel(eq("schedule_id"), scheduleCancelCaptor.capture());
    assertEquals(false, scheduleCancelCaptor.getValue().getProrate());
    assertEquals(false, scheduleCancelCaptor.getValue().getInvoiceNow());
    verify(subscriptionServiceMock).cancel(eq("sub_active"), any(SubscriptionCancelParams.class));
    assertNotNull(actual);
  }

  @SneakyThrows
  @Test
  void cancel_all_scheduled_subscriptions_immediately_by_default() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();

    var earliestStartEpoch = now().plus(1L, DAYS).getEpochSecond();
    var latestStartEpoch = now().plus(2L, DAYS).getEpochSecond();
    var earliestSchedule =
        someNotStartedSchedule("earliest_schedule_id", earliestStartEpoch, "price_base");
    var latestSchedule =
        someNotStartedSchedule("latest_schedule_id", latestStartEpoch, "price_upgraded");
    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    when(scheduleStripeCollectionMock.getData())
        .thenReturn(List.of(earliestSchedule, latestSchedule));
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    var activeStripeSubscription = new com.stripe.model.Subscription();
    activeStripeSubscription.setId("sub_active");
    activeStripeSubscription.setStatus("active");
    activeStripeSubscription.setCurrentPeriodStart(now().getEpochSecond());
    activeStripeSubscription.setCurrentPeriodEnd(now().plusSeconds(3600).getEpochSecond());
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of(activeStripeSubscription));

    var subscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    when(stripeClientMock.subscriptions()).thenReturn(subscriptionServiceMock);

    var actual = subject.cancelLatestUserSubscription(user);

    // Every pending phase is dropped, not only the earliest one.
    var scheduleIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(subscriptionScheduleServiceMock, times(2))
        .cancel(scheduleIdCaptor.capture(), any(SubscriptionScheduleCancelParams.class));
    assertEquals(
        List.of("earliest_schedule_id", "latest_schedule_id"), scheduleIdCaptor.getAllValues());
    verify(earliestSchedule, never()).update(any(SubscriptionScheduleUpdateParams.class));
    verify(latestSchedule, never()).update(any(SubscriptionScheduleUpdateParams.class));
    verify(subscriptionServiceMock).cancel(eq("sub_active"), any(SubscriptionCancelParams.class));
    assertNotNull(actual);
  }

  @SneakyThrows
  @Test
  void cancel_scheduled_subscription_immediately_without_running_subscription() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();

    var scheduledStartEpoch = now().plus(1L, DAYS).getEpochSecond();
    var schedule = someNotStartedSchedule("schedule_id", scheduledStartEpoch, "price_base");
    when(schedule.getCreated()).thenReturn(now().getEpochSecond());
    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of(schedule));
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    // Only the not-started schedule exists: there is no running subscription to cancel.
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of());

    var actual = subject.cancelLatestUserSubscription(user);

    verify(subscriptionScheduleServiceMock)
        .cancel(eq("schedule_id"), any(SubscriptionScheduleCancelParams.class));
    verify(userSubscriptionProductServiceMock)
        .endActiveSubscriptionProducts(eq("user_id"), any(Instant.class));
    assertNotNull(actual);
  }

  @SneakyThrows
  @Test
  void cancel_current_subscription_skips_stripe_canceled_subscription() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();

    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of());
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    var periodEndEpoch = now().plusSeconds(3600).getEpochSecond();
    var canceledStripeSubscription = new com.stripe.model.Subscription();
    canceledStripeSubscription.setId("sub_canceled");
    canceledStripeSubscription.setStatus("canceled");
    canceledStripeSubscription.setCurrentPeriodStart(now().minusSeconds(7200).getEpochSecond());
    canceledStripeSubscription.setCurrentPeriodEnd(periodEndEpoch);
    var activeStripeSubscription = new com.stripe.model.Subscription();
    activeStripeSubscription.setId("sub_active");
    activeStripeSubscription.setStatus("active");
    activeStripeSubscription.setCurrentPeriodStart(now().getEpochSecond());
    activeStripeSubscription.setCurrentPeriodEnd(periodEndEpoch);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of(canceledStripeSubscription, activeStripeSubscription));

    var subscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    when(stripeClientMock.subscriptions()).thenReturn(subscriptionServiceMock);

    var actual = subject.cancelLatestUserSubscription(user, END_OF_PERIOD);

    var idCaptor = ArgumentCaptor.forClass(String.class);
    verify(subscriptionServiceMock).update(idCaptor.capture(), any(SubscriptionUpdateParams.class));
    assertEquals("sub_active", idCaptor.getValue());
    assertNotNull(actual);
  }

  @SneakyThrows
  @Test
  void cancel_stripe_canceled_subscription_only_ko() {
    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of());
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    var canceledStripeSubscription = new com.stripe.model.Subscription();
    canceledStripeSubscription.setId("sub_canceled");
    canceledStripeSubscription.setStatus("canceled");
    canceledStripeSubscription.setCurrentPeriodStart(now().getEpochSecond());
    canceledStripeSubscription.setCurrentPeriodEnd(now().plusSeconds(3600).getEpochSecond());
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of(canceledStripeSubscription));

    var subscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    when(stripeClientMock.subscriptions()).thenReturn(subscriptionServiceMock);

    var actual =
        assertThrows(
            IllegalStateException.class,
            () ->
                subject.cancelLatestUserSubscription(
                    User.builder().id("user_id").userSubscriptionId("customer_id").build()));

    assertEquals(
        "Only active subscription can be cancelled but none of the 1 subscription(s) is active",
        actual.getMessage());
    verify(subscriptionServiceMock, never())
        .update(anyString(), any(SubscriptionUpdateParams.class));
  }

  @SneakyThrows
  @Test
  void cancel_current_subscription_without_period_end_falls_back_to_now() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();

    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of());
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    // Active subscription without a currentPeriodEnd -> domain endDatetime is null.
    var activeStripeSubscription = new com.stripe.model.Subscription();
    activeStripeSubscription.setId("sub_active");
    activeStripeSubscription.setStatus("active");
    activeStripeSubscription.setCurrentPeriodStart(now().getEpochSecond());
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of(activeStripeSubscription));

    var subscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    when(stripeClientMock.subscriptions()).thenReturn(subscriptionServiceMock);

    var actual = subject.cancelLatestUserSubscription(user, END_OF_PERIOD);

    // Falls back to cancel time (a non-null Instant) when the period end is unknown.
    verify(userSubscriptionProductServiceMock)
        .endActiveSubscriptionProducts(eq("user_id"), any(Instant.class));
    assertNotNull(actual);
  }

  @SneakyThrows
  @Test
  void cancel_after_invoice_paid_cancels_flagged_schedule_immediately() {
    var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    var subscription = mock(com.stripe.model.Subscription.class);
    when(subscription.getSchedule()).thenReturn("schedule_id");
    when(stripeSubscriptionServiceMock.retrieve("sub_active")).thenReturn(subscription);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);

    var scheduleServiceMock = mock(SubscriptionScheduleService.class);
    var schedule = mock(SubscriptionSchedule.class);
    when(schedule.getMetadata()).thenReturn(Map.of("cancel_after_first_invoice", "true"));
    when(schedule.getStatus()).thenReturn("active");
    when(scheduleServiceMock.retrieve("schedule_id")).thenReturn(schedule);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(scheduleServiceMock);

    subject.cancelScheduledSubscriptionAfterInvoicePaid("sub_active");

    var paramsCaptor = ArgumentCaptor.forClass(SubscriptionScheduleCancelParams.class);
    verify(scheduleServiceMock).cancel(eq("schedule_id"), paramsCaptor.capture());
    // The just-collected payment is kept: no proration credit and no extra invoice.
    assertEquals(false, paramsCaptor.getValue().getProrate());
    assertEquals(false, paramsCaptor.getValue().getInvoiceNow());
  }

  @SneakyThrows
  @Test
  void cancel_after_invoice_paid_ignores_schedule_without_flag() {
    var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    var subscription = mock(com.stripe.model.Subscription.class);
    when(subscription.getSchedule()).thenReturn("schedule_id");
    when(stripeSubscriptionServiceMock.retrieve("sub_active")).thenReturn(subscription);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);

    var scheduleServiceMock = mock(SubscriptionScheduleService.class);
    var schedule = mock(SubscriptionSchedule.class);
    when(schedule.getMetadata()).thenReturn(Map.of()); // no cancellation flag
    when(scheduleServiceMock.retrieve("schedule_id")).thenReturn(schedule);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(scheduleServiceMock);

    subject.cancelScheduledSubscriptionAfterInvoicePaid("sub_active");

    verify(scheduleServiceMock, never()).cancel(any(), any(SubscriptionScheduleCancelParams.class));
  }

  @SneakyThrows
  @Test
  void cancel_after_invoice_paid_ignores_schedule_with_null_metadata() {
    var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    var subscription = mock(com.stripe.model.Subscription.class);
    when(subscription.getSchedule()).thenReturn("schedule_id");
    when(stripeSubscriptionServiceMock.retrieve("sub_active")).thenReturn(subscription);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);

    var scheduleServiceMock = mock(SubscriptionScheduleService.class);
    var schedule = mock(SubscriptionSchedule.class);
    when(schedule.getMetadata()).thenReturn(null);
    when(scheduleServiceMock.retrieve("schedule_id")).thenReturn(schedule);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(scheduleServiceMock);

    subject.cancelScheduledSubscriptionAfterInvoicePaid("sub_active");

    verify(scheduleServiceMock, never()).cancel(any(), any(SubscriptionScheduleCancelParams.class));
  }

  @SneakyThrows
  @Test
  void cancel_after_invoice_paid_skips_already_canceled_schedule() {
    var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    var subscription = mock(com.stripe.model.Subscription.class);
    when(subscription.getSchedule()).thenReturn("schedule_id");
    when(stripeSubscriptionServiceMock.retrieve("sub_active")).thenReturn(subscription);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);

    var scheduleServiceMock = mock(SubscriptionScheduleService.class);
    var schedule = mock(SubscriptionSchedule.class);
    when(schedule.getMetadata()).thenReturn(Map.of("cancel_after_first_invoice", "true"));
    when(schedule.getStatus()).thenReturn("canceled");
    when(scheduleServiceMock.retrieve("schedule_id")).thenReturn(schedule);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(scheduleServiceMock);

    subject.cancelScheduledSubscriptionAfterInvoicePaid("sub_active");

    verify(scheduleServiceMock, never()).cancel(any(), any(SubscriptionScheduleCancelParams.class));
  }

  @SneakyThrows
  @Test
  void cancel_after_invoice_paid_noop_when_subscription_not_scheduled() {
    var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    var subscription = mock(com.stripe.model.Subscription.class);
    when(subscription.getSchedule()).thenReturn(null); // not managed by a schedule
    when(stripeSubscriptionServiceMock.retrieve("sub_active")).thenReturn(subscription);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);

    subject.cancelScheduledSubscriptionAfterInvoicePaid("sub_active");

    verify(stripeClientMock, never()).subscriptionSchedules();
  }

  @SneakyThrows
  @Test
  void cancel_after_invoice_paid_noop_when_no_subscription_id() {
    subject.cancelScheduledSubscriptionAfterInvoicePaid(null);

    verify(stripeClientMock, never()).subscriptions();
  }

  @SneakyThrows
  @Test
  void cancel_after_invoice_paid_propagates_stripe_errors() {
    var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    when(stripeSubscriptionServiceMock.retrieve("sub_active"))
        .thenThrow(new com.stripe.exception.ApiConnectionException("stripe down"));
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);

    assertThrows(
        com.stripe.exception.ApiConnectionException.class,
        () -> subject.cancelScheduledSubscriptionAfterInvoicePaid("sub_active"));
  }

  @SneakyThrows
  @Test
  void cancel_inactive_subscription_ko() {
    var stripeCustomerWithNonActiveSubscriptionId = "stripeCustomerWithNonActiveSubscriptionId";
    var inactiveStripeSubscription = new com.stripe.model.Subscription();
    inactiveStripeSubscription.setStatus("unknown");
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of());
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId(any()))
        .thenReturn(List.of(inactiveStripeSubscription));

    var actualInactiveSubscriptionException =
        assertThrows(
            IllegalStateException.class,
            () ->
                subject.cancelLatestUserSubscription(
                    User.builder()
                        .userSubscriptionId(stripeCustomerWithNonActiveSubscriptionId)
                        .build()));

    assertEquals(
        "Only active subscription can be cancelled but none of the 1 subscription(s) is active",
        actualInactiveSubscriptionException.getMessage());
  }

  @SneakyThrows
  @Test
  void get_subscription_reports_cancelled_status_for_flagged_schedule() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();
    var scheduledStartEpoch = now().plus(1L, DAYS).getEpochSecond();
    var flaggedSchedule = someNotStartedSchedule("schedule_id", scheduledStartEpoch, "price_base");
    when(flaggedSchedule.getCreated()).thenReturn(now().getEpochSecond());
    when(flaggedSchedule.getMetadata()).thenReturn(Map.of("cancel_after_first_invoice", "true"));
    mockActiveSchedules(flaggedSchedule);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of());
    when(subscriptionEligibleJpaRepositoryMock.findByUserId("user_id"))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .userId("user_id")
                    .trialPeriodDays(0)
                    .eligibleFrom(LocalDate.now().minusDays(10L))
                    .build()));

    var latest = subject.getSubscriptionByUser(user).getLatestSubscription();

    assertEquals(Subscription.SubscriptionStatus.CANCELED, latest.getStatus());
    assertTrue(latest.isActive()); // still served until the schedule start date
  }

  @SneakyThrows
  @Test
  void get_subscription_reports_active_status_for_unflagged_schedule() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();
    var scheduledStartEpoch = now().plus(1L, DAYS).getEpochSecond();
    var schedule = someNotStartedSchedule("schedule_id", scheduledStartEpoch, "price_base");
    when(schedule.getCreated()).thenReturn(now().getEpochSecond());
    when(schedule.getMetadata()).thenReturn(Map.of()); // not flagged for cancellation
    mockActiveSchedules(schedule);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of());
    when(subscriptionEligibleJpaRepositoryMock.findByUserId("user_id"))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .userId("user_id")
                    .trialPeriodDays(0)
                    .eligibleFrom(LocalDate.now().minusDays(10L))
                    .build()));

    var latest = subject.getSubscriptionByUser(user).getLatestSubscription();

    assertEquals(Subscription.SubscriptionStatus.ACTIVE, latest.getStatus());
    assertTrue(latest.isActive());
  }

  @SneakyThrows
  @Test
  void initiate_subscription_allowed_when_current_schedule_is_pending_cancellation() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();
    // The user still has an active (not-yet-started) schedule, but it has already been flagged for
    // cancellation after its first invoice, so it must not block a new subscription initiation.
    var scheduledStartEpoch = now().plus(1L, DAYS).getEpochSecond();
    var flaggedSchedule = someNotStartedSchedule("schedule_id", scheduledStartEpoch, "price_base");
    when(flaggedSchedule.getCreated()).thenReturn(now().getEpochSecond());
    when(flaggedSchedule.getMetadata()).thenReturn(Map.of("cancel_after_first_invoice", "true"));
    mockActiveSchedules(flaggedSchedule);
    // No running Stripe subscription: the only thing surfacing as active is the flagged schedule.
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of());
    when(subscriptionEligibleJpaRepositoryMock.findByUserId("user_id"))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .userId("user_id")
                    .trialPeriodDays(0)
                    .eligibleFrom(LocalDate.now().minusDays(10L))
                    .build()));

    var expectedRedirection = mockInitiateSubscriptionDependencies(user);

    var actual =
        subject.initiateSubscription(
            user, someSubscriptionToInitiate(), getRedirectionStatusUrls());

    assertSame(expectedRedirection, actual);
  }

  @SneakyThrows
  @Test
  void initiate_subscription_allowed_when_active_subscription_is_cancelled_at_period_end() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();
    // No schedule, but a running Stripe subscription already set to cancel at period end: it
    // surfaces
    // as CANCELED (still active until period end) and must not block a new subscription initiation.
    mockActiveSchedules(); // no scheduled subscription
    var cancelledStripeSubscription = new com.stripe.model.Subscription();
    cancelledStripeSubscription.setId("sub_cancelled");
    cancelledStripeSubscription.setStatus("active");
    cancelledStripeSubscription.setCancelAtPeriodEnd(true);
    cancelledStripeSubscription.setCurrentPeriodStart(now().getEpochSecond());
    cancelledStripeSubscription.setCurrentPeriodEnd(now().plus(20L, DAYS).getEpochSecond());
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of(cancelledStripeSubscription));
    when(subscriptionEligibleJpaRepositoryMock.findByUserId("user_id"))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .userId("user_id")
                    .trialPeriodDays(0)
                    .eligibleFrom(LocalDate.now().minusDays(10L))
                    .build()));

    var expectedRedirection = mockInitiateSubscriptionDependencies(user);

    var actual =
        subject.initiateSubscription(
            user, someSubscriptionToInitiate(), getRedirectionStatusUrls());

    assertSame(expectedRedirection, actual);
  }

  @SneakyThrows
  @Test
  void initiate_subscription_anchors_billing_cycle_on_first_day_of_next_month() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();
    mockActiveSchedules();
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of());
    when(subscriptionEligibleJpaRepositoryMock.findByUserId("user_id"))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .userId("user_id")
                    .trialPeriodDays(0)
                    .eligibleFrom(LocalDate.now().minusDays(10L))
                    .build()));
    mockInitiateSubscriptionDependencies(user);

    subject.initiateSubscription(user, someSubscriptionToInitiate(), getRedirectionStatusUrls());

    assertEquals(expectedBillingCycleAnchor(), capturedBillingCycleAnchor());
  }

  @SneakyThrows
  @Test
  void initiate_subscription_starts_today_even_when_trial_period_is_still_running() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();
    mockActiveSchedules();
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of());
    when(subscriptionEligibleJpaRepositoryMock.findByUserId("user_id"))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .userId("user_id")
                    .trialPeriodDays(40)
                    .eligibleFrom(LocalDate.now())
                    .build()));
    mockInitiateSubscriptionDependencies(user);

    subject.initiateSubscription(user, someSubscriptionToInitiate(), getRedirectionStatusUrls());

    assertEquals(expectedBillingCycleAnchor(), capturedBillingCycleAnchor());
  }

  @SneakyThrows
  @Test
  void initiate_subscription_does_not_create_any_extra_analysis_price() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();
    mockActiveSchedules();
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of());
    when(subscriptionEligibleJpaRepositoryMock.findByUserId("user_id"))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .userId("user_id")
                    .trialPeriodDays(0)
                    .eligibleFrom(LocalDate.now().minusDays(10L))
                    .build()));
    mockInitiateSubscriptionDependencies(user);

    subject.initiateSubscription(user, someSubscriptionToInitiate(), getRedirectionStatusUrls());

    verify(stripeClientMock, never()).prices();
  }

  private Long capturedBillingCycleAnchor() throws StripeException {
    var anchorCaptor = ArgumentCaptor.forClass(Long.class);
    verify(sessionFactoryMock)
        .initiateSubscriptionWorkflow(any(), any(), anchorCaptor.capture(), any());
    return anchorCaptor.getValue();
  }

  private static long expectedBillingCycleAnchor() {
    var today = LocalDate.now();
    var firstFullBillingPeriodStart =
        today.getDayOfMonth() == 1 ? today : today.withDayOfMonth(1).plusMonths(1);
    return firstFullBillingPeriodStart.atStartOfDay(ZoneId.of("Europe/Paris")).toEpochSecond();
  }

  @SneakyThrows
  @Test
  void initiate_subscription_blocked_when_current_schedule_is_not_pending_cancellation() {
    var user = User.builder().id("user_id").userSubscriptionId("customer_id").build();
    var scheduledStartEpoch = now().plus(1L, DAYS).getEpochSecond();
    var activeSchedule = someNotStartedSchedule("schedule_id", scheduledStartEpoch, "price_base");
    when(activeSchedule.getCreated()).thenReturn(now().getEpochSecond());
    when(activeSchedule.getMetadata()).thenReturn(Map.of()); // not flagged for cancellation
    mockActiveSchedules(activeSchedule);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("customer_id"))
        .thenReturn(List.of());
    when(subscriptionEligibleJpaRepositoryMock.findByUserId("user_id"))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .userId("user_id")
                    .trialPeriodDays(0)
                    .eligibleFrom(LocalDate.now().minusDays(10L))
                    .build()));
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices("customer_id")).thenReturn(List.of());
    when(stripeCustomerServiceMock.getCustomer(user)).thenReturn(mock(Customer.class));

    var subscriptionToInitiate = someSubscriptionToInitiate();
    var redirectionUrls = getRedirectionStatusUrls();
    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.initiateSubscription(user, subscriptionToInitiate, redirectionUrls));

    assertTrue(actual.getMessage().startsWith("User.id=user_id has active subscription until "));
    verify(sessionFactoryMock, never())
        .initiateSubscriptionWorkflow(any(), any(), anyLong(), any());
  }

  private void mockActiveSchedules(SubscriptionSchedule... schedules) throws StripeException {
    var scheduleServiceMock = mock(SubscriptionScheduleService.class);
    StripeCollection<SubscriptionSchedule> scheduleCollectionMock = mock();
    when(scheduleCollectionMock.getData()).thenReturn(List.of(schedules));
    when(scheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(scheduleServiceMock);
  }

  private Redirection mockInitiateSubscriptionDependencies(User user) throws StripeException {
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices("customer_id")).thenReturn(List.of());
    when(stripeCustomerServiceMock.getCustomer(user)).thenReturn(mock(Customer.class));
    var expectedRedirection = new Redirection();
    when(sessionFactoryMock.initiateSubscriptionWorkflow(any(), any(), anyLong(), any()))
        .thenReturn(expectedRedirection);
    return expectedRedirection;
  }

  private static Subscription someSubscriptionToInitiate() {
    return Subscription.builder()
        .subscriptionProduct(SubscriptionProduct.builder().e2Id("plan_e2_id").build())
        .endDatetime(now().plus(30L, DAYS))
        .build();
  }

  private static RedirectionStatusUrls getRedirectionStatusUrls() {
    return new RedirectionStatusUrls()
        .successUrl("http://localhost/success")
        .failureUrl("http://localhost/failure");
  }

  @SneakyThrows
  @Test
  void create_or_link_user_and_make_user_eligible_to_subscription_check() {
    var userId = "userId";
    var userEmail = "userEmail";
    var userMock = User.builder().id(userId).email(userEmail).build();
    var stripeSubscriptionService = mock(com.stripe.service.SubscriptionService.class);
    var stripeCollectionMock = mock(StripeCollection.class);
    var stripeCustomerServiceMock = mock(CustomerService.class);
    var customerStripeCollectionMock = mock(StripeCollection.class);
    StripeCollection<SubscriptionSchedule> scheduleStripeCollectionMock = mock();
    var subscriptionScheduleServiceMock = mock(SubscriptionScheduleService.class);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userMock.getId()))
        .thenReturn(Optional.empty());
    when(subscriptionEligibleJpaRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(userRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(stripeCollectionMock.getData()).thenReturn(List.of());
    when(stripeSubscriptionService.list(any(SubscriptionListParams.class)))
        .thenReturn(stripeCollectionMock);
    when(scheduleStripeCollectionMock.getData()).thenReturn(List.of());
    when(subscriptionScheduleServiceMock.list(any(SubscriptionScheduleListParams.class)))
        .thenReturn(scheduleStripeCollectionMock);
    when(stripeClientMock.subscriptionSchedules()).thenReturn(subscriptionScheduleServiceMock);

    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionService);
    when(customerStripeCollectionMock.getData()).thenReturn(List.of(new Customer()));
    when(stripeCustomerServiceMock.list(any(CustomerListParams.class)))
        .thenReturn(customerStripeCollectionMock);
    when(stripeClientMock.customers()).thenReturn(stripeCustomerServiceMock);

    var actual = subject.createOrLinkUserSubscription(userMock);

    assertNotNull(actual);
    var userSubscriptionEligibleCaptor = ArgumentCaptor.forClass(UserSubscriptionEligible.class);
    verify(subscriptionEligibleJpaRepositoryMock).save(userSubscriptionEligibleCaptor.capture());
    var userSubscriptionEligible = userSubscriptionEligibleCaptor.getValue();
    assertNotNull(userSubscriptionEligible.getId());
    assertEquals(userMock.getId(), userSubscriptionEligible.getUserId());
  }

  @Test
  void add_consumption() {
    when(consumptionLogJpaRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    var userId = "userId";
    var usageMetric = 2L;
    var instantNow = now();
    var subscriptionConsumption =
        SubscriptionConsumptionLog.builder()
            .id(randomUUID().toString())
            .userId(userId)
            .consumptionType(ROOF_ANALYSIS)
            .usageMetric(usageMetric)
            .consumptionUnit(UNIT)
            .creationDatetime(instantNow)
            .build();

    var actual = subject.addConsumption(subscriptionConsumption);

    assertEquals(subscriptionConsumption, actual);
  }

  @SneakyThrows
  @Test
  void compute_monthly_subscription_variable_consumption_is_never_reported_to_stripe() {
    var userId = randomUUID().toString();
    var expectedUsage = 22L; // above the former free roof analysis threshold
    var expected = List.of(new ConsumptionUsageSummary(ROOF_ANALYSIS, expectedUsage));
    var userMock = mock(User.class);
    var usageRecordMockedStatic = mockStatic(UsageRecord.class);
    when(userMock.getId()).thenReturn(userId);
    when(detectionTrackingJpaRepositoryMock.findAllByIdUserAndCreationDatetimeBetween(
            userId, temporalUtils.startOfLastMonthInstant(), temporalUtils.endOfLastMonthInstant()))
        .thenReturn(someDetectionTrackingLogs(userId, (int) expectedUsage));

    var actual = subject.computeMonthlySubscriptionVariableConsumption(userMock);

    assertEquals(expected, actual);
    usageRecordMockedStatic.verify(
        () ->
            UsageRecord.createOnSubscriptionItem(
                any(String.class), any(UsageRecordCreateOnSubscriptionItemParams.class)),
        times(0));
    verify(stripeClientMock, never()).subscriptions();
    verify(stripeClientMock, never()).subscriptionItems();

    usageRecordMockedStatic.close();
  }

  @SneakyThrows
  @Test
  void do_not_compute_monthly_subscription_variable_consumption_when_empty_logs() {
    var userId = randomUUID().toString();
    var expectedUsage = 0L;
    var expected = List.of();

    var userMock = mock(User.class);
    var usageRecordMockedStatic = mockStatic(UsageRecord.class);

    when(userMock.getId()).thenReturn(userId);
    when(detectionTrackingJpaRepositoryMock.findAllByIdUserAndCreationDatetimeBetween(
            userId, temporalUtils.startOfLastMonthInstant(), temporalUtils.endOfLastMonthInstant()))
        .thenReturn(someDetectionTrackingLogs(userId, (int) expectedUsage));

    var actual = subject.computeMonthlySubscriptionVariableConsumption(userMock);

    assertEquals(expected, actual);
    usageRecordMockedStatic.verify(
        () ->
            UsageRecord.createOnSubscriptionItem(
                any(String.class), any(UsageRecordCreateOnSubscriptionItemParams.class)),
        times(0));

    usageRecordMockedStatic.close();
  }

  private @NotNull List<HDetectionTracking> someDetectionTrackingLogs(
      String userId, int totalUsageExpected) {
    var logs = new ArrayList<HDetectionTracking>();
    for (int i = 0; i < totalUsageExpected; i++) {
      logs.add(someDetectionTracking(userId, now()));
    }
    return logs;
  }

  @Test
  void update_auto_renewal_status_persists_a_new_history_entry() {
    var userId = randomUUID().toString();
    var commitmentId = randomUUID().toString();
    var commitment =
        app.bpartners.api.model.UserSubscriptionCommitment.builder()
            .id(commitmentId)
            .userId(userId)
            .build();
    when(userSubscriptionCommitmentJpaRepositoryMock.findById(commitmentId))
        .thenReturn(Optional.of(commitment));

    subject.updateUserSubscriptionCommitmentAutoRenewalStatus(
        userId, commitmentId, app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED);

    var historyCaptor =
        ArgumentCaptor.forClass(
            app.bpartners.api.model.UserSubscriptionCommitmentAutoRenewalStatusHistory.class);
    verify(userSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepositoryMock)
        .save(historyCaptor.capture());
    var savedHistory = historyCaptor.getValue();
    assertNotNull(savedHistory.getId());
    assertEquals(commitmentId, savedHistory.getUserSubscriptionCommitmentId());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED,
        savedHistory.getAutoRenewalStatus());
    assertNotNull(savedHistory.getCreationDatetime());
    // the commitment row itself is never mutated: history is append-only
    verify(userSubscriptionCommitmentJpaRepositoryMock, never()).save(any());
  }

  @Test
  void update_auto_renewal_status_ko_when_status_is_null() {
    assertThrows(
        BadRequestException.class,
        () ->
            subject.updateUserSubscriptionCommitmentAutoRenewalStatus(
                randomUUID().toString(), randomUUID().toString(), null));
    verifyNoInteractions(
        userSubscriptionCommitmentJpaRepositoryMock,
        userSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepositoryMock);
  }

  @Test
  void update_auto_renewal_status_ko_when_commitment_not_owned_by_user() {
    var userId = randomUUID().toString();
    var commitmentId = randomUUID().toString();
    var commitment =
        app.bpartners.api.model.UserSubscriptionCommitment.builder()
            .id(commitmentId)
            .userId(randomUUID().toString())
            .build();
    when(userSubscriptionCommitmentJpaRepositoryMock.findById(commitmentId))
        .thenReturn(Optional.of(commitment));

    assertThrows(
        NotFoundException.class,
        () ->
            subject.updateUserSubscriptionCommitmentAutoRenewalStatus(
                userId, commitmentId, app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED));
    verifyNoInteractions(userSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepositoryMock);
  }

  private static Price somePrice(String priceId, String productE2Id, String recurringInterval) {
    Price price = mock(Price.class);
    lenient().when(price.getId()).thenReturn(priceId);
    lenient().when(price.getProduct()).thenReturn(productE2Id);
    if (recurringInterval != null) {
      var recurring = mock(Price.Recurring.class);
      lenient().when(recurring.getInterval()).thenReturn(recurringInterval);
      lenient().when(price.getRecurring()).thenReturn(recurring);
    }
    return price;
  }

  private static com.stripe.model.Subscription someStripeSubscriptionBilling(Price... prices) {
    var items = new ArrayList<SubscriptionItem>();
    for (var price : prices) {
      var item = mock(SubscriptionItem.class);
      lenient().when(item.getPrice()).thenReturn(price);
      items.add(item);
    }
    SubscriptionItemCollection itemCollectionMock = mock();
    lenient().when(itemCollectionMock.getData()).thenReturn(items);
    var stripeSubscription = mock(com.stripe.model.Subscription.class);
    lenient().when(stripeSubscription.getItems()).thenReturn(itemCollectionMock);
    return stripeSubscription;
  }

  private void mockSubscribablePlanAndMeteredProduct() {
    when(subscriptionProductRepositoryMock.findByE2Id("plan_e2_id"))
        .thenReturn(
            Optional.of(
                SubscriptionProduct.builder()
                    .id("plan_id")
                    .e2Id("plan_e2_id")
                    .billingType(SubscriptionBillingType.COMMITMENT)
                    .annualE2PriceId("annual_price_id")
                    .build()));
    lenient()
        .when(subscriptionProductRepositoryMock.findByE2Id("metered_e2_id"))
        .thenReturn(
            Optional.of(
                SubscriptionProduct.builder().id("metered_id").e2Id("metered_e2_id").build()));
  }

  @Test
  void resolve_subscribed_plan_reports_monthly_billing_interval() {
    mockSubscribablePlanAndMeteredProduct();
    var stripeSubscription =
        someStripeSubscriptionBilling(
            somePrice("metered_price_id", "metered_e2_id", "month"),
            somePrice("monthly_price_id", "plan_e2_id", "month"));

    var actual = subject.resolveSubscribedPlan(stripeSubscription).orElseThrow();

    assertEquals("plan_id", actual.planId());
    assertEquals(BillingInterval.MONTHLY, actual.billingInterval());
  }

  @Test
  void resolve_subscribed_plan_reports_yearly_billing_interval() {
    mockSubscribablePlanAndMeteredProduct();
    var stripeSubscription =
        someStripeSubscriptionBilling(somePrice("annual_price_id", "plan_e2_id", "year"));

    var actual = subject.resolveSubscribedPlan(stripeSubscription).orElseThrow();

    assertEquals("plan_id", actual.planId());
    assertEquals(BillingInterval.YEARLY, actual.billingInterval());
  }

  @Test
  void resolve_subscribed_plan_reports_yearly_billing_interval_from_plan_annual_price() {
    mockSubscribablePlanAndMeteredProduct();
    var stripeSubscription =
        someStripeSubscriptionBilling(somePrice("annual_price_id", "plan_e2_id", null));

    var actual = subject.resolveSubscribedPlan(stripeSubscription).orElseThrow();

    assertEquals(BillingInterval.YEARLY, actual.billingInterval());
  }

  @Test
  void resolve_subscribed_plan_of_a_schedule_reports_its_billing_interval() throws StripeException {
    mockSubscribablePlanAndMeteredProduct();
    var scheduleItem = mock(SubscriptionSchedule.Phase.Item.class);
    when(scheduleItem.getPrice()).thenReturn("annual_price_id");
    var phase = mock(SubscriptionSchedule.Phase.class);
    when(phase.getItems()).thenReturn(List.of(scheduleItem));
    var schedule = mock(SubscriptionSchedule.class);
    when(schedule.getPhases()).thenReturn(List.of(phase));
    var annualPrice = somePrice("annual_price_id", "plan_e2_id", "year");
    var priceServiceMock = mock(com.stripe.service.PriceService.class);
    when(priceServiceMock.retrieve("annual_price_id")).thenReturn(annualPrice);
    when(stripeClientMock.prices()).thenReturn(priceServiceMock);

    var actual = subject.resolveSubscribedPlan(schedule).orElseThrow();

    assertEquals("plan_id", actual.planId());
    assertEquals(BillingInterval.YEARLY, actual.billingInterval());
  }

  @Test
  void resolve_subscribed_plan_is_empty_without_subscribable_plan() {
    mockSubscribablePlanAndMeteredProduct();
    var stripeSubscription =
        someStripeSubscriptionBilling(somePrice("metered_price_id", "metered_e2_id", "month"));

    assertTrue(subject.resolveSubscribedPlan(stripeSubscription).isEmpty());
  }
}
