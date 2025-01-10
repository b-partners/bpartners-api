package app.bpartners.api.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionConsumptionLogJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.MonthUtils;
import com.stripe.StripeClient;
import com.stripe.model.Customer;
import com.stripe.model.StripeCollection;
import com.stripe.param.CustomerListParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.service.CustomerService;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionServiceTest {
  StripeConf stripeConfMock = mock();
  StripeClient stripeClientMock = mock();
  UserRepository userRepositoryMock = mock();
  SubscriptionProductRepository productRepositoryMock = mock();
  UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepositoryMock = mock();
  SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepositoryMock = mock();
  SubscriptionService subject =
      new SubscriptionService(
          stripeConfMock,
          stripeClientMock,
          userRepositoryMock,
          productRepositoryMock,
          subscriptionEligibleJpaRepositoryMock,
          new MonthUtils(),
          consumptionLogJpaRepositoryMock);

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
}
