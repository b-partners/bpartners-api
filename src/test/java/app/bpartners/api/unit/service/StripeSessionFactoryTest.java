package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.subscription.StripeSessionFactory;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionScheduleCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class StripeSessionFactoryTest {

  StripeSessionFactory subject;

  @BeforeEach
  void setup() {
    subject = new StripeSessionFactory();
  }

  @Test
  void create_session_subscription_should_build_and_create_session() throws StripeException {
    Customer customer = new Customer();
    customer.setId("cus_123");
    SubscriptionProduct product =
        SubscriptionProduct.builder().e2Id("prod_abc").priceInCents(1000L).type(MONTHLY).build();
    Price variablePrice = new Price();
    variablePrice.setId("price_metered");
    RedirectionStatusUrls urls =
        new RedirectionStatusUrls()
            .successUrl("http://success.url")
            .failureUrl("http://cancel.url");
    long billingAnchor = 1710000000L;

    try (MockedStatic<Session> sessionStatic = Mockito.mockStatic(Session.class)) {
      Session fakeSession = mock(Session.class);
      sessionStatic
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(fakeSession);

      Session result =
          subject.createSessionSubscription(customer, product, variablePrice, urls, billingAnchor);

      assertEquals(fakeSession, result);
      sessionStatic.verify(() -> Session.create(any(SessionCreateParams.class)), times(1));
    }
  }

  @Test
  void create_session_setUp_should_create_session_and_simulate_schedule() throws StripeException {
    Customer customer = new Customer();
    customer.setId("cus_456");
    SubscriptionProduct product =
        SubscriptionProduct.builder().e2Id("prod_xyz").priceInCents(2000L).type(MONTHLY).build();
    Subscription subscription = Subscription.builder().subscriptionProduct(product).build();
    Price variablePrice = new Price();
    variablePrice.setId("price_var");
    RedirectionStatusUrls urls =
        new RedirectionStatusUrls().successUrl("http://ok.url").failureUrl("http://fail.url");
    long billingAnchor = 1711111111L;
    try (MockedStatic<Session> sessionMocked = mockStatic(Session.class);
        MockedStatic<SubscriptionSchedule> scheduleMocked =
            mockStatic(SubscriptionSchedule.class)) {
      Session fakeSession = mock(Session.class);
      SubscriptionSchedule fakeSchedule = mock(SubscriptionSchedule.class);
      sessionMocked
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(fakeSession);
      scheduleMocked
          .when(() -> SubscriptionSchedule.create(any(SubscriptionScheduleCreateParams.class)))
          .thenReturn(fakeSchedule);

      Session result =
          subject.createSessionSetUp(customer, urls, subscription, variablePrice, billingAnchor);

      assertEquals(fakeSession, result);
      sessionMocked.verify(() -> Session.create(any(SessionCreateParams.class)), times(1));
      scheduleMocked.verify(
          () -> SubscriptionSchedule.create(any(SubscriptionScheduleCreateParams.class)), times(1));
    }
  }

  @Test
  void computeRecurring_should_throw_for_unknown_type() {
    SubscriptionProduct product = SubscriptionProduct.builder().type(null).build();
    Subscription subscription = Subscription.builder().subscriptionProduct(product).build();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            subject.createSessionSetUp(
                new Customer(), new RedirectionStatusUrls(), subscription, new Price(), 0L));
  }
}
