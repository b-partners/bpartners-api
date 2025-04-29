package app.bpartners.api.unit.service;

import static java.time.Month.APRIL;
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.subscription.StripeSessionFactory;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

public class StripeSessionFactoryTest {

  @Mock private TemporalUtils temporalUtils;
  @InjectMocks private StripeSessionFactory stripeSessionFactorySpy;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    stripeSessionFactorySpy = Mockito.spy(new StripeSessionFactory(temporalUtils));
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
    doReturn(mockSession)
        .when(stripeSessionFactorySpy)
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor);

    var actual =
        stripeSessionFactorySpy.createSession(
            trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(stripeSessionFactorySpy, times(1))
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor);
    verify(stripeSessionFactorySpy, never())
        .createSessionSubscription(any(), any(), any(), any(), any());
    assertThat(actual).isEqualTo(mockSession);
  }

  @Test
  void create_session_if_trial_end_is_before_end_of_actual_month_not_between_1_and_4_of_month()
      throws StripeException {
    var trialEnd = LocalDate.of(2025, APRIL, 10);
    mockTemporalWindow();
    when(temporalUtils.endOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 30));
    var mockSession = mock(Session.class);
    var customer = mock(Customer.class);
    var product = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    doReturn(mockSession)
        .when(stripeSessionFactorySpy)
        .createSessionSubscription(customer, product, price, urls, billingCycleAnchor);

    var actual =
        stripeSessionFactorySpy.createSession(
            trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(stripeSessionFactorySpy, times(1))
        .createSessionSubscription(customer, product, price, urls, billingCycleAnchor);
    verify(stripeSessionFactorySpy, never()).createSessionSetUp(any(), any(), any(), any(), any());
    assertThat(actual).isEqualTo(mockSession);
  }

  @Test
  void
      create_session_if_trial_end_is_before_end_of_next_month_not_between_1_and_4_of_month_and_after_this_month()
          throws StripeException {
    var trialEnd = LocalDate.of(2025, MAY, 10);
    mockTemporalWindow();
    when(temporalUtils.endOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 30));
    var mockSession = mock(Session.class);
    var customer = mock(Customer.class);
    var product = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    doReturn(mockSession)
        .when(stripeSessionFactorySpy)
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor);

    var actual =
        stripeSessionFactorySpy.createSession(
            trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(stripeSessionFactorySpy, times(1)).createSessionSetUp(any(), any(), any(), any(), any());
    verify(stripeSessionFactorySpy, never())
        .createSessionSubscription(any(), any(), any(), any(), any());
    assertThat(actual).isEqualTo(mockSession);
  }

  @Test
  void create_session_if_trial_end_is_between_1_to_4_next_month() throws StripeException {
    var trialEnd = LocalDate.of(2025, MAY, 2);
    mockTemporalWindow();
    when(temporalUtils.endOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 30));
    var mockSession = mock(Session.class);
    var customer = mock(Customer.class);
    var product = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    doReturn(mockSession)
        .when(stripeSessionFactorySpy)
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor);

    var actual =
        stripeSessionFactorySpy.createSession(
            trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(stripeSessionFactorySpy, times(1))
        .createSessionSetUp(customer, urls, subscription, price, billingCycleAnchor);
    verify(stripeSessionFactorySpy, never())
        .createSessionSubscription(any(), any(), any(), any(), any());
    assertThat(actual).isEqualTo(mockSession);
  }

  private void mockTemporalWindow() {
    // Setup mocks for trialEnd checks
    when(temporalUtils.startOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 1));
    when(temporalUtils.fourthOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 4));

    when(temporalUtils.startOfNextMonth()).thenReturn(LocalDate.of(2025, MAY, 1));
    when(temporalUtils.fourthOfNextMonth()).thenReturn(LocalDate.of(2025, MAY, 4));
  }
}
