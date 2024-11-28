package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.UserSubscriptionType;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.service.PriceService;
import com.stripe.service.ProductService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubscriptionServiceTest {

  private static final long DEFAULT_FREE_TRIAL_DAYS = 0L;
  StripeConf stripeConfMock = mock(StripeConf.class);
  StripeClient stripeClientMock = mock(StripeClient.class);
  UserRepository userRepositoryMock = mock(UserRepository.class);
  SubscriptionProductRepository subscriptionProductRepositoryMock =
      mock(SubscriptionProductRepository.class);
  UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepositoryMock =
      mock(UserSubscriptionEligibleJpaRepository.class);
  SubscriptionService subject =
      new SubscriptionService(
          stripeConfMock,
          stripeClientMock,
          userRepositoryMock,
          subscriptionProductRepositoryMock,
          subscriptionEligibleJpaRepositoryMock);

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
            .freeTrialDays(DEFAULT_FREE_TRIAL_DAYS)
            .build();
    assertEquals(expected, actual);
  }
}
