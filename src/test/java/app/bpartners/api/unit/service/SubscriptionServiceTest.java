package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.UserSubscriptionType;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionConsumptionLogJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.StripeCollection;
import com.stripe.param.SubscriptionListParams;
import com.stripe.service.PriceService;
import com.stripe.service.ProductService;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

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
}
