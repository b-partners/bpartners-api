package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.UserSubscriptionType;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.*;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionConsumptionLogJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.CustomerListParams;
import com.stripe.param.SubscriptionItemListParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.UsageRecordCreateOnSubscriptionItemParams;
import com.stripe.service.CustomerService;
import com.stripe.service.PriceService;
import com.stripe.service.ProductService;
import com.stripe.service.SubscriptionItemService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionServiceTest {
  StripeConf stripeConfMock = mock(StripeConf.class);
  StripeClient stripeClientMock = mock(StripeClient.class);
  UserRepository userRepositoryMock = mock(UserRepository.class);
  SubscriptionProductRepository subscriptionProductRepositoryMock =
      mock(SubscriptionProductRepository.class);
  UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepositoryMock =
      mock(UserSubscriptionEligibleJpaRepository.class);
  SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepository =
      mock(SubscriptionConsumptionLogJpaRepository.class);
  TemporalUtils temporalUtils = new TemporalUtils();
  SubscriptionService subject =
      new SubscriptionService(
          stripeConfMock,
          stripeClientMock,
          userRepositoryMock,
          subscriptionProductRepositoryMock,
          subscriptionEligibleJpaRepositoryMock,
          temporalUtils,
          consumptionLogJpaRepository);

  @Test
  void get_subscription_consumption_logs_ok() {
    var userId = randomUUID().toString();
    var startOfMonth = temporalUtils.startOfMonth();
    var endOfMonth = temporalUtils.endOfMonth();

    var expected = List.of(someConsumptionLog(userId, now()));
    when(consumptionLogJpaRepository.findAllByUserIdAndCreationDatetimeBetween(
            userId, startOfMonth, endOfMonth))
        .thenReturn(expected);

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

  @Test
  void get_by_subscription_type_ok() throws StripeException {
    var userSubscriptionType = UserSubscriptionType.ESSENTIAL;
    when(stripeConfMock.getEssentialSubscriptionProductId())
        .thenReturn("esentialSubscriptionProductId");
    when(subscriptionProductRepositoryMock.findById(any()))
        .thenReturn(Optional.ofNullable(mock(SubscriptionProduct.class)));
    var product = new Product();
    var products = mock(ProductService.class);
    when(stripeClientMock.products()).thenReturn(products);
    when(products.retrieve(any())).thenReturn(product);
    product.setDefaultPrice("");
    product.setMarketingFeatures(List.of(new Product.MarketingFeature()));
    product.setImages(List.of("image"));
    product.setCreated(1L);
    var price = new Price();
    var prices = mock(PriceService.class);
    when(stripeClientMock.prices()).thenReturn(prices);
    when(prices.retrieve(any())).thenReturn(price);
    var recurring = new Price.Recurring();
    price.setRecurring(recurring);
    recurring.setInterval("month");

    var actual = subject.getBySubscriptionType(userSubscriptionType);

    var subscriptionProduct =
        SubscriptionProduct.builder()
            .id(actual.getSubscriptionProduct().getId())
            .e2Id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .features(
                product.getMarketingFeatures().stream()
                    .map(Product.MarketingFeature::getName)
                    .toList())
            .priceInCents(price.getUnitAmount())
            .imageUrl(product.getImages().getFirst())
            .type(MONTHLY)
            .creationDatetime(actual.getSubscriptionProduct().getCreationDatetime())
            .build();
    var expected =
        Subscription.builder()
            .subscriptionProduct(subscriptionProduct)
            .endDatetime(actual.getEndDatetime())
            .build();
    assertEquals(expected, actual);
  }

  @SneakyThrows
  @Test
  void cancel_subscription_ko() {
    var stripeCustomerWithEmptySubscriptionId = "stripeCustomerWithEmptySubscriptionId";
    var stripeSubscriptionServiceMock1 = mock(com.stripe.service.SubscriptionService.class);
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
  void cancel_inactive_subscription_ko() {
    var stripeCustomerWithNonActiveSubscriptionId = "stripeCustomerWithNonActiveSubscriptionId";
    var inactiveStripeSubscription = new com.stripe.model.Subscription();
    inactiveStripeSubscription.setStatus("unknown");
    var stripeSubscriptionServiceMock1 = mock(com.stripe.service.SubscriptionService.class);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock1);
    var stripeCollectionMock = mock(StripeCollection.class);
    when(stripeSubscriptionServiceMock1.list(any(SubscriptionListParams.class)))
        .thenReturn(stripeCollectionMock);
    when(stripeCollectionMock.getData()).thenReturn(List.of(inactiveStripeSubscription));

    var actualInactiveSubscriptionException =
        assertThrows(
            IllegalStateException.class,
            () ->
                subject.cancelLatestUserSubscription(
                    User.builder()
                        .userSubscriptionId(stripeCustomerWithNonActiveSubscriptionId)
                        .build()));

    assertEquals(
        "Only active subscription can be cancelled but actual status is UNKNOWN",
        actualInactiveSubscriptionException.getMessage());
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
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userMock.getId()))
        .thenReturn(Optional.empty());
    when(subscriptionEligibleJpaRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(userRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(stripeCollectionMock.getData()).thenReturn(List.of());
    when(stripeSubscriptionService.list(any(SubscriptionListParams.class)))
        .thenReturn(stripeCollectionMock);
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
    when(consumptionLogJpaRepository.save(any()))
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
  void compute_monthly_subscription_variable_consumption_with_payable_usage() {
    var userId = randomUUID().toString();
    var subscriptionProductE2Id = "subscriptionProductE2Id";
    var stripeSubscriptionItemMockId = "stripeSubscriptionItemMockId";
    var expectedUsage = 22L;
    var expectedPayableUsage = 2L; // 20L are free roof analysis
    var expected = List.of(new ConsumptionUsageSummary(ROOF_ANALYSIS, expectedUsage));
    var stripeProductId = "stripeProductId";

    var userMock = mock(User.class);
    var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    var stripeProductServiceMock = mock(ProductService.class);
    var stripeSubscriptionCollectionMock = mock(StripeCollection.class);
    var subscriptionProductMock = mock(SubscriptionProduct.class);
    var stripeSubscriptionMock = mock(com.stripe.model.Subscription.class);
    var stripeSubscriptionItemsCollectionMock = mock(StripeCollection.class);
    var stripeSubscriptionItemMock = mock(SubscriptionItem.class);
    var subscriptionItemServiceMock = mock(SubscriptionItemService.class);
    var stripePriceMock = mock(Price.class);
    var stripeProductMock = mock(Product.class);
    var usageRecordMockedStatic = mockStatic(UsageRecord.class);

    when(userMock.getId()).thenReturn(userId);
    when(stripeSubscriptionItemMock.getId()).thenReturn(stripeSubscriptionItemMockId);
    when(subscriptionProductMock.getE2Id()).thenReturn(subscriptionProductE2Id);
    when(stripeProductMock.getId()).thenReturn(subscriptionProductE2Id);
    when(stripePriceMock.getProduct()).thenReturn(stripeProductId);
    when(stripeSubscriptionItemMock.getPrice()).thenReturn(stripePriceMock);
    when(stripeSubscriptionMock.getCurrentPeriodStart()).thenReturn(Instant.now().getEpochSecond());
    when(stripeSubscriptionCollectionMock.getData()).thenReturn(List.of(stripeSubscriptionMock));
    when(stripeSubscriptionItemsCollectionMock.getData())
        .thenReturn(List.of(stripeSubscriptionItemMock));
    when(stripeSubscriptionServiceMock.list(any(SubscriptionListParams.class)))
        .thenReturn(stripeSubscriptionCollectionMock);
    when(subscriptionItemServiceMock.list(any(SubscriptionItemListParams.class)))
        .thenReturn(stripeSubscriptionItemsCollectionMock);
    when(stripeProductServiceMock.retrieve(stripeProductId)).thenReturn(stripeProductMock);
    when(stripeClientMock.products()).thenReturn(stripeProductServiceMock);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);
    when(stripeClientMock.subscriptionItems()).thenReturn(subscriptionItemServiceMock);
    when(consumptionLogJpaRepository.findAllByUserIdAndCreationDatetimeBetween(
            userId, temporalUtils.startOfMonth(), temporalUtils.endOfMonth()))
        .thenReturn(someSubscriptionConsumptionLogs(userId, (int) expectedUsage));
    when(subscriptionProductRepositoryMock.findByConsumptionTypeAttached(ROOF_ANALYSIS))
        .thenReturn(subscriptionProductMock);
    var stripeUsageRecordCreateCaptor =
        ArgumentCaptor.forClass(UsageRecordCreateOnSubscriptionItemParams.class);
    usageRecordMockedStatic
        .when(
            () ->
                UsageRecord.createOnSubscriptionItem(
                    eq(stripeSubscriptionItemMockId), stripeUsageRecordCreateCaptor.capture()))
        .thenReturn(null);

    var actual = subject.computeMonthlySubscriptionVariableConsumption(userMock);

    var stripeUsageRecordCreated = stripeUsageRecordCreateCaptor.getValue();
    assertEquals(expected, actual);
    assertEquals(expectedPayableUsage, stripeUsageRecordCreated.getQuantity());
    assertNotNull(stripeUsageRecordCreated.getTimestamp());

    usageRecordMockedStatic.close();
  }

  @SneakyThrows
  @Test
  void compute_monthly_subscription_variable_consumption_without_payable_usage() {
    var userId = randomUUID().toString();
    var subscriptionProductE2Id = "subscriptionProductE2Id";
    var stripeSubscriptionItemMockId = "stripeSubscriptionItemMockId";
    var stripeProductId = "stripeProductId";
    var expectedUsage = 2L;
    while (expectedUsage <= 20L) {
      var expected = List.of(new ConsumptionUsageSummary(ROOF_ANALYSIS, expectedUsage));

      var userMock = mock(User.class);
      var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
      var stripeSubscriptionCollectionMock = mock(StripeCollection.class);
      var subscriptionProductMock = mock(SubscriptionProduct.class);
      var stripeSubscriptionMock = mock(com.stripe.model.Subscription.class);
      var stripeSubscriptionItemsCollectionMock = mock(StripeCollection.class);
      var stripeSubscriptionItemMock = mock(SubscriptionItem.class);
      var subscriptionItemServiceMock = mock(SubscriptionItemService.class);
      var stripePriceMock = mock(Price.class);
      var stripeProductMock = mock(Product.class);
      var usageRecordMockedStatic = mockStatic(UsageRecord.class);
      var stripeProductServiceMock = mock(ProductService.class);

      when(userMock.getId()).thenReturn(userId);
      when(stripeSubscriptionItemMock.getId()).thenReturn(stripeSubscriptionItemMockId);
      when(subscriptionProductMock.getE2Id()).thenReturn(subscriptionProductE2Id);
      when(stripeProductMock.getId()).thenReturn(subscriptionProductE2Id);
      when(stripePriceMock.getProduct()).thenReturn(stripeProductId);
      when(stripeSubscriptionItemMock.getPrice()).thenReturn(stripePriceMock);
      when(stripeSubscriptionMock.getCurrentPeriodStart())
          .thenReturn(Instant.now().getEpochSecond());
      when(stripeSubscriptionCollectionMock.getData()).thenReturn(List.of(stripeSubscriptionMock));
      when(stripeSubscriptionItemsCollectionMock.getData())
          .thenReturn(List.of(stripeSubscriptionItemMock));
      when(stripeSubscriptionServiceMock.list(any(SubscriptionListParams.class)))
          .thenReturn(stripeSubscriptionCollectionMock);
      when(subscriptionItemServiceMock.list(any(SubscriptionItemListParams.class)))
          .thenReturn(stripeSubscriptionItemsCollectionMock);
      when(stripeProductServiceMock.retrieve(stripeProductId)).thenReturn(stripeProductMock);
      when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);
      when(stripeClientMock.subscriptionItems()).thenReturn(subscriptionItemServiceMock);
      when(consumptionLogJpaRepository.findAllByUserIdAndCreationDatetimeBetween(
              userId, temporalUtils.startOfMonth(), temporalUtils.endOfMonth()))
          .thenReturn(someSubscriptionConsumptionLogs(userId, (int) expectedUsage));
      when(subscriptionProductRepositoryMock.findByConsumptionTypeAttached(ROOF_ANALYSIS))
          .thenReturn(subscriptionProductMock);

      var actual = subject.computeMonthlySubscriptionVariableConsumption(userMock);

      assertEquals(expected, actual);
      usageRecordMockedStatic.verify(
          () ->
              UsageRecord.createOnSubscriptionItem(
                  any(String.class), any(UsageRecordCreateOnSubscriptionItemParams.class)),
          times(0));

      usageRecordMockedStatic.close();
      expectedUsage = expectedUsage + 2L;
    }
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
    when(consumptionLogJpaRepository.findAllByUserIdAndCreationDatetimeBetween(
            userId, temporalUtils.startOfMonth(), temporalUtils.endOfMonth()))
        .thenReturn(someSubscriptionConsumptionLogs(userId, (int) expectedUsage));

    var actual = subject.computeMonthlySubscriptionVariableConsumption(userMock);

    assertEquals(expected, actual);
    usageRecordMockedStatic.verify(
        () ->
            UsageRecord.createOnSubscriptionItem(
                any(String.class), any(UsageRecordCreateOnSubscriptionItemParams.class)),
        times(0));

    usageRecordMockedStatic.close();
  }

  @SneakyThrows
  @Test
  void compute_monthly_subscription_variable_consumption_ko_without_stripe_subscription() {
    var userId = randomUUID().toString();
    var expectedUsage = 22L;

    var userMock = mock(User.class);
    var subscriptionProductMock = mock(SubscriptionProduct.class);
    var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    var stripeSubscriptionCollectionMock = mock(StripeCollection.class);

    when(userMock.getId()).thenReturn(userId);
    when(stripeSubscriptionCollectionMock.getData())
        .thenReturn(List.of()); // Subscription from stripe empty
    when(stripeSubscriptionServiceMock.list(any(SubscriptionListParams.class)))
        .thenReturn(stripeSubscriptionCollectionMock);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);

    when(consumptionLogJpaRepository.findAllByUserIdAndCreationDatetimeBetween(
            userId, temporalUtils.startOfMonth(), temporalUtils.endOfMonth()))
        .thenReturn(someSubscriptionConsumptionLogs(userId, (int) expectedUsage));
    when(subscriptionProductRepositoryMock.findByConsumptionTypeAttached(ROOF_ANALYSIS))
        .thenReturn(subscriptionProductMock);

    var actual =
        assertThrows(
            NotFoundException.class,
            () -> subject.computeMonthlySubscriptionVariableConsumption(userMock));

    assertEquals("Any subscription found for User.id=" + userId, actual.getMessage());
  }

  @SneakyThrows
  @Test
  void compute_monthly_subscription_variable_consumption_ko_without_stripe_subscription_item() {
    var userId = randomUUID().toString();
    var expectedUsage = 22L;

    var userMock = mock(User.class);
    var stripeSubscriptionServiceMock = mock(com.stripe.service.SubscriptionService.class);
    var stripeSubscriptionCollectionMock = mock(StripeCollection.class);
    var subscriptionProductMock = mock(SubscriptionProduct.class);
    var stripeSubscriptionMock = mock(com.stripe.model.Subscription.class);
    var stripeSubscriptionItemsCollectionMock = mock(StripeCollection.class);
    var subscriptionItemServiceMock = mock(SubscriptionItemService.class);
    when(userMock.getId()).thenReturn(userId);

    when(stripeSubscriptionMock.getCurrentPeriodStart()).thenReturn(Instant.now().getEpochSecond());
    when(stripeSubscriptionCollectionMock.getData()).thenReturn(List.of(stripeSubscriptionMock));
    when(stripeSubscriptionItemsCollectionMock.getData())
        .thenReturn(List.of()); // Subscription item from stripe empty
    when(stripeSubscriptionServiceMock.list(any(SubscriptionListParams.class)))
        .thenReturn(stripeSubscriptionCollectionMock);
    when(subscriptionItemServiceMock.list(any(SubscriptionItemListParams.class)))
        .thenReturn(stripeSubscriptionItemsCollectionMock);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionServiceMock);
    when(stripeClientMock.subscriptionItems()).thenReturn(subscriptionItemServiceMock);

    when(consumptionLogJpaRepository.findAllByUserIdAndCreationDatetimeBetween(
            userId, temporalUtils.startOfMonth(), temporalUtils.endOfMonth()))
        .thenReturn(someSubscriptionConsumptionLogs(userId, (int) expectedUsage));
    when(subscriptionProductRepositoryMock.findByConsumptionTypeAttached(ROOF_ANALYSIS))
        .thenReturn(subscriptionProductMock);

    var actual =
        assertThrows(
            NotFoundException.class,
            () -> subject.computeMonthlySubscriptionVariableConsumption(userMock));

    assertEquals(
        "Any SubscriptionItem matches to SubscriptionProduct for User.id=" + userId,
        actual.getMessage());
  }

  private @NotNull List<SubscriptionConsumptionLog> someSubscriptionConsumptionLogs(
      String userId, int totalUsageExpected) {
    var logs = new ArrayList<SubscriptionConsumptionLog>();
    for (int i = 0; i < totalUsageExpected; i++) {
      logs.add(someConsumptionLog(userId, now()));
    }
    return logs;
  }
}
