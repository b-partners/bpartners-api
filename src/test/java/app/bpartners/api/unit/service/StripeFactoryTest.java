package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static java.time.Month.APRIL;
import static java.time.Month.MAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.subscription.StripeFactory;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

public class StripeFactoryTest {

  @Mock private TemporalUtils temporalUtilsMock;
  @InjectMocks private StripeFactory subject;
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

    subject = spy(new StripeFactory(temporalUtilsMock));
  }

  @Test
  void initiate_subscription_workflow_redirects_to_checkout_session() throws StripeException {
    var customer = mock(Customer.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    var session = mock(Session.class);
    when(session.getUrl()).thenReturn("checkout_url");
    doReturn(session)
        .when(subject)
        .createSessionSubscription(customer, subscription, urls, billingCycleAnchor);

    var actual =
        subject.initiateSubscriptionWorkflow(customer, urls, billingCycleAnchor, subscription);

    verify(subject, times(1))
        .createSessionSubscription(customer, subscription, urls, billingCycleAnchor);
    assertEquals("checkout_url", actual.getRedirectionUrl());
    assertEquals(urls, actual.getRedirectionStatusUrls());
  }

  @Test
  void create_session_subscription_bills_the_partial_month_prorata() throws StripeException {
    mockToday();
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn("customer_id");
    var urls = mock(RedirectionStatusUrls.class);
    when(urls.getSuccessUrl()).thenReturn("success_url");
    when(urls.getFailureUrl()).thenReturn("failure_url");
    var product = mock(SubscriptionProduct.class);
    when(product.getE2Id()).thenReturn("e2id");
    when(product.getPriceInCentsWithVat()).thenReturn(5880L);
    when(product.getType()).thenReturn(MONTHLY);
    var subscription = mock(Subscription.class);
    when(subscription.getSubscriptionProduct()).thenReturn(product);
    when(subscription.getBillingInterval()).thenReturn(BillingInterval.MONTHLY);
    var firstOfNextMonth = parisMidnight(LocalDate.of(2025, MAY, 1));
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      subject.createSessionSubscription(customer, subscription, urls, firstOfNextMonth);

      var captor = ArgumentCaptor.forClass(SessionCreateParams.class);
      mockedSession.verify(() -> Session.create(captor.capture()));
      var subscriptionData = captor.getValue().getSubscriptionData();
      assertEquals(
          SessionCreateParams.SubscriptionData.ProrationBehavior.CREATE_PRORATIONS,
          subscriptionData.getProrationBehavior());
      assertEquals(firstOfNextMonth, subscriptionData.getBillingCycleAnchor());
    }
  }

  @Test
  void create_session_subscription_sets_no_anchor_when_billing_cycle_starts_today()
      throws StripeException {
    when(temporalUtilsMock.today()).thenReturn(LocalDate.of(2025, APRIL, 1));
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn("customer_id");
    var urls = mock(RedirectionStatusUrls.class);
    when(urls.getSuccessUrl()).thenReturn("success_url");
    when(urls.getFailureUrl()).thenReturn("failure_url");
    var product = mock(SubscriptionProduct.class);
    when(product.getE2Id()).thenReturn("e2id");
    when(product.getPriceInCentsWithVat()).thenReturn(5880L);
    when(product.getType()).thenReturn(MONTHLY);
    var subscription = mock(Subscription.class);
    when(subscription.getSubscriptionProduct()).thenReturn(product);
    when(subscription.getBillingInterval()).thenReturn(BillingInterval.MONTHLY);
    var firstOfActualMonth = parisMidnight(LocalDate.of(2025, APRIL, 1));
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      subject.createSessionSubscription(customer, subscription, urls, firstOfActualMonth);

      var captor = ArgumentCaptor.forClass(SessionCreateParams.class);
      mockedSession.verify(() -> Session.create(captor.capture()));
      assertNull(captor.getValue().getSubscriptionData());
    }
  }

  @Test
  void create_session_subscription_yearly_charges_the_whole_year_at_checkout()
      throws StripeException {
    mockToday();
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn("customer_id");
    var urls = mock(RedirectionStatusUrls.class);
    when(urls.getSuccessUrl()).thenReturn("success_url");
    when(urls.getFailureUrl()).thenReturn("failure_url");
    var product = mock(SubscriptionProduct.class);
    when(product.getAnnualE2PriceId()).thenReturn("annual_price_id");
    var subscription = mock(Subscription.class);
    when(subscription.getSubscriptionProduct()).thenReturn(product);
    when(subscription.getBillingInterval()).thenReturn(BillingInterval.YEARLY);
    var firstOfNextMonth = parisMidnight(LocalDate.of(2025, MAY, 1));
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      subject.createSessionSubscription(customer, subscription, urls, firstOfNextMonth);

      var captor = ArgumentCaptor.forClass(SessionCreateParams.class);
      mockedSession.verify(() -> Session.create(captor.capture()));
      assertNull(captor.getValue().getSubscriptionData());
    }
  }

  @Test
  void create_session_subscription_monthly_only_bills_the_fixed_plan() throws StripeException {
    mockToday();
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn("customer_id");
    var urls = mock(RedirectionStatusUrls.class);
    when(urls.getSuccessUrl()).thenReturn("success_url");
    when(urls.getFailureUrl()).thenReturn("failure_url");
    var product = mock(SubscriptionProduct.class);
    when(product.getE2Id()).thenReturn("e2id");
    when(product.getPriceInCentsWithVat()).thenReturn(5880L);
    when(product.getType()).thenReturn(MONTHLY);
    var subscription = mock(Subscription.class);
    when(subscription.getSubscriptionProduct()).thenReturn(product);
    when(subscription.getBillingInterval()).thenReturn(BillingInterval.MONTHLY);
    var firstOfNextMonth = parisMidnight(LocalDate.of(2025, MAY, 1));
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      subject.createSessionSubscription(customer, subscription, urls, firstOfNextMonth);

      var captor = ArgumentCaptor.forClass(SessionCreateParams.class);
      mockedSession.verify(() -> Session.create(captor.capture()));
      var lineItems = captor.getValue().getLineItems();
      assertEquals(1, lineItems.size());
      assertEquals("e2id", lineItems.getFirst().getPriceData().getProduct());
    }
  }

  @Test
  void create_session_subscription_yearly_uses_annual_price_only() throws StripeException {
    mockToday();
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn("customer_id");
    var urls = mock(RedirectionStatusUrls.class);
    when(urls.getSuccessUrl()).thenReturn("success_url");
    when(urls.getFailureUrl()).thenReturn("failure_url");
    var product = mock(SubscriptionProduct.class);
    when(product.getAnnualE2PriceId()).thenReturn("annual_price_id");
    var subscription = mock(Subscription.class);
    when(subscription.getSubscriptionProduct()).thenReturn(product);
    when(subscription.getBillingInterval()).thenReturn(BillingInterval.YEARLY);
    var billingCycleAnchor = parisMidnight(LocalDate.of(2025, MAY, 1));
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      subject.createSessionSubscription(customer, subscription, urls, billingCycleAnchor);

      var captor = ArgumentCaptor.forClass(SessionCreateParams.class);
      mockedSession.verify(() -> Session.create(captor.capture()));
      var lineItems = captor.getValue().getLineItems();
      assertEquals(1, lineItems.size());
      assertEquals("annual_price_id", lineItems.getFirst().getPrice());
      assertNull(lineItems.getFirst().getPriceData());
    }
  }

  private void mockToday() {
    when(temporalUtilsMock.today()).thenReturn(LocalDate.of(2025, APRIL, 10));
  }

  private long parisMidnight(LocalDate date) {
    return date.atStartOfDay(ZoneId.of("Europe/Paris")).toEpochSecond();
  }
}
