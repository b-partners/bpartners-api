package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static java.time.Month.APRIL;
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionSession;
import app.bpartners.api.repository.jpa.UserSubscriptionSessionRepository;
import app.bpartners.api.service.subscription.StripeSessionFactory;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

public class StripeSessionFactoryTest {

  @Mock private TemporalUtils temporalUtilsMock;
  @InjectMocks private StripeSessionFactory stripeSessionFactorySpy;
  @Mock private UserSubscriptionSessionRepository userSubscriptionSessionRepositoryMock;
  @Mock private Session stripeSessionMock;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      when(stripeSessionMock.getId()).thenReturn("session_id");
    }
    stripeSessionFactorySpy =
        spy(new StripeSessionFactory(temporalUtilsMock, userSubscriptionSessionRepositoryMock));
  }

  @Test
  void create_session_set_up_ok() throws StripeException {
    var customer = mock(Customer.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var price = mock(Price.class);
    var billingCycleAnchor = 123456L;
    var user = User.builder().id("user_id").build();
    var product = mock(SubscriptionProduct.class);

    when(customer.getId()).thenReturn("customer_id");
    when(urls.getSuccessUrl()).thenReturn("success_url");
    when(urls.getFailureUrl()).thenReturn("failure_url");
    when(price.getId()).thenReturn("price_id");
    when(subscription.getSubscriptionProduct()).thenReturn(product);
    when(product.getE2Id()).thenReturn("e2id");
    when(product.getPriceInCents()).thenReturn(5880L);
    when(product.getType()).thenReturn(MONTHLY);
    Session fakeSession = new Session();
    fakeSession.setId("session_id");
    com.stripe.model.SubscriptionSchedule fakeSchedule =
        new com.stripe.model.SubscriptionSchedule();
    fakeSchedule.setId("schedule_id");

    try (MockedStatic<Session> mockedSession = Mockito.mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(fakeSession);
      doReturn(fakeSchedule)
          .when(stripeSessionFactorySpy)
          .subscriptionScheduleCreation(
              anyString(), any(Subscription.class), anyString(), anyLong());

      Session result =
          stripeSessionFactorySpy.createSessionSetUp(
              customer, urls, subscription, price, billingCycleAnchor, user);

      assertThat(result.getId()).isEqualTo("session_id");
      mockedSession.verify(() -> Session.create(any(SessionCreateParams.class)), times(1));
      verify(userSubscriptionSessionRepositoryMock, times(1))
          .save(any(UserSubscriptionSession.class));
    }
  }

  @Test
  void create_session_if_trial_end_is_between_1_to_4_actual_month() throws StripeException {
    var trialEnd = LocalDate.of(2025, APRIL, 2);
    mockTemporalWindow();
    var mockSession = mock(Session.class);
    var customer = mock(Customer.class);
    var product = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    var user = User.builder().id("user_id").build();
    doReturn(mockSession)
        .when(stripeSessionFactorySpy)
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor, user);

    var actual =
        stripeSessionFactorySpy.createSession(
            user, trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(stripeSessionFactorySpy, times(1))
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor, user);
    verify(stripeSessionFactorySpy, never())
        .createSessionSubscription(any(), any(), any(), any(), any());
    assertThat(actual).isEqualTo(mockSession);
  }

  @Test
  void create_session_if_trial_end_is_before_end_of_actual_month_not_between_1_and_4_of_month()
      throws StripeException {
    var trialEnd = LocalDate.of(2025, APRIL, 10);
    mockTemporalWindow();
    when(temporalUtilsMock.endOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 30));
    var mockSession = mock(Session.class);
    var customer = mock(Customer.class);
    var product = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    var user = User.builder().id("user_id").build();
    doReturn(mockSession)
        .when(stripeSessionFactorySpy)
        .createSessionSubscription(customer, product, price, urls, billingCycleAnchor);

    var actual =
        stripeSessionFactorySpy.createSession(
            user, trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(stripeSessionFactorySpy, times(1))
        .createSessionSubscription(customer, product, price, urls, billingCycleAnchor);
    verify(stripeSessionFactorySpy, never())
        .createSessionSetUp(any(), any(), any(), any(), any(), any());
    assertThat(actual).isEqualTo(mockSession);
  }

  @Test
  void
      create_session_if_trial_end_is_before_end_of_next_month_not_between_1_and_4_of_month_and_after_this_month()
          throws StripeException {
    var trialEnd = LocalDate.of(2025, MAY, 10);
    mockTemporalWindow();
    when(temporalUtilsMock.endOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 30));
    var mockSession = mock(Session.class);
    var customer = mock(Customer.class);
    var product = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    var user = User.builder().build();
    doReturn(mockSession)
        .when(stripeSessionFactorySpy)
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor, user);

    var actual =
        stripeSessionFactorySpy.createSession(
            user, trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(stripeSessionFactorySpy, times(1))
        .createSessionSetUp(any(), any(), any(), any(), any(), any());
    verify(stripeSessionFactorySpy, never())
        .createSessionSubscription(any(), any(), any(), any(), any());
    assertThat(actual).isEqualTo(mockSession);
  }

  @Test
  void create_session_if_trial_end_is_between_1_to_4_next_month() throws StripeException {
    var trialEnd = LocalDate.of(2025, MAY, 2);
    mockTemporalWindow();
    when(temporalUtilsMock.endOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 30));
    var mockSession = mock(Session.class);
    var customer = mock(Customer.class);
    var product = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    var user = User.builder().id("user_id").build();
    doReturn(mockSession)
        .when(stripeSessionFactorySpy)
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor, user);

    var actual =
        stripeSessionFactorySpy.createSession(
            user, trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(stripeSessionFactorySpy, times(1))
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor, user);
    verify(stripeSessionFactorySpy, never())
        .createSessionSubscription(any(), any(), any(), any(), any());
    assertThat(actual).isEqualTo(mockSession);
  }

  private void mockTemporalWindow() {
    // Setup mocks for trialEnd checks
    when(temporalUtilsMock.startOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 1));
    when(temporalUtilsMock.fourthOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 4));

    when(temporalUtilsMock.startOfNextMonth()).thenReturn(LocalDate.of(2025, MAY, 1));
    when(temporalUtilsMock.fourthOfNextMonth()).thenReturn(LocalDate.of(2025, MAY, 4));
  }
}
