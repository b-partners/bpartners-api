package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static app.bpartners.api.service.subscription.StripeFactory.OVERAGE_BILLING_CYCLE_ANCHOR_METADATA_KEY;
import static app.bpartners.api.service.subscription.StripeFactory.OVERAGE_METERED_PRICE_ID_METADATA_KEY;
import static java.time.Month.APRIL;
import static java.time.Month.MAY;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionSession;
import app.bpartners.api.repository.jpa.UserSubscriptionSessionRepository;
import app.bpartners.api.service.subscription.StripeFactory;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.model.SubscriptionScheduleCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionScheduleCreateParams;
import com.stripe.param.SubscriptionScheduleListParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

public class StripeFactoryTest {

  @Mock private TemporalUtils temporalUtilsMock;
  @InjectMocks private StripeFactory subject;
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

    subject = spy(new StripeFactory(temporalUtilsMock, userSubscriptionSessionRepositoryMock));
  }

  @Test
  void initiate_subscription_workflow_redirects_to_checkout_session() throws StripeException {
    var customer = mock(Customer.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    var session = mock(Session.class);
    when(session.getUrl()).thenReturn("checkout_url");
    doReturn(session)
        .when(subject)
        .createSessionSubscription(customer, subscription, price, urls, billingCycleAnchor);

    var actual =
        subject.initiateSubscriptionWorkflow(
            customer, price, urls, billingCycleAnchor, subscription);

    verify(subject, times(1))
        .createSessionSubscription(customer, subscription, price, urls, billingCycleAnchor);
    assertEquals("checkout_url", actual.getRedirectionUrl());
    assertEquals(urls, actual.getRedirectionStatusUrls());
  }

  @Test
  void initiate_subscription_workflow_yearly_does_not_schedule_overage_before_payment()
      throws StripeException {
    var customer = mock(Customer.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    doReturn(mock(Session.class))
        .when(subject)
        .createSessionSubscription(customer, subscription, price, urls, billingCycleAnchor);

    subject.initiateSubscriptionWorkflow(customer, price, urls, billingCycleAnchor, subscription);

    verify(subject, never()).scheduleOverageSubscription(any(), any(), any(), any());
    verify(userSubscriptionSessionRepositoryMock, never()).save(any(UserSubscriptionSession.class));
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
    var meteredPrice = mock(Price.class);
    var firstOfNextMonth = parisMidnight(LocalDate.of(2025, MAY, 1));
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      subject.createSessionSubscription(
          customer, subscription, meteredPrice, urls, firstOfNextMonth);

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
    var meteredPrice = mock(Price.class);
    var firstOfActualMonth = parisMidnight(LocalDate.of(2025, APRIL, 1));
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      subject.createSessionSubscription(
          customer, subscription, meteredPrice, urls, firstOfActualMonth);

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
    var meteredPrice = mock(Price.class);
    when(meteredPrice.getId()).thenReturn("metered_price_id");
    var firstOfNextMonth = parisMidnight(LocalDate.of(2025, MAY, 1));
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      subject.createSessionSubscription(
          customer, subscription, meteredPrice, urls, firstOfNextMonth);

      var captor = ArgumentCaptor.forClass(SessionCreateParams.class);
      mockedSession.verify(() -> Session.create(captor.capture()));
      var subscriptionData = captor.getValue().getSubscriptionData();
      assertNull(subscriptionData.getBillingCycleAnchor());
      assertNull(subscriptionData.getProrationBehavior());
      assertEquals(
          "metered_price_id",
          subscriptionData.getMetadata().get(OVERAGE_METERED_PRICE_ID_METADATA_KEY));
      assertEquals(
          String.valueOf(firstOfNextMonth),
          subscriptionData.getMetadata().get(OVERAGE_BILLING_CYCLE_ANCHOR_METADATA_KEY));
    }
  }

  @Test
  void create_session_subscription_yearly_uses_annual_price_and_no_metered_line_item()
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
    var meteredPrice = mock(Price.class);
    var billingCycleAnchor = parisMidnight(LocalDate.of(2025, MAY, 1));
    try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
      mockedSession
          .when(() -> Session.create(any(SessionCreateParams.class)))
          .thenReturn(stripeSessionMock);

      subject.createSessionSubscription(
          customer, subscription, meteredPrice, urls, billingCycleAnchor);

      var captor = ArgumentCaptor.forClass(SessionCreateParams.class);
      mockedSession.verify(() -> Session.create(captor.capture()));
      var lineItems = captor.getValue().getLineItems();
      assertEquals(1, lineItems.size());
      assertEquals("annual_price_id", lineItems.getFirst().getPrice());
      assertNull(lineItems.getFirst().getPriceData());
    }
  }

  @Test
  void schedule_overage_subscription_saves_the_session() {
    var billingCycleAnchor = 123456L;
    var user = User.builder().id("user_id").build();
    var overageSchedule = new SubscriptionSchedule();
    overageSchedule.setId("overage_schedule_id");
    doReturn(overageSchedule)
        .when(subject)
        .overageScheduleCreation(anyString(), anyString(), anyLong());

    var noExistingSchedule = someScheduleCollection();
    try (MockedStatic<SubscriptionSchedule> mockedSchedule =
        mockStatic(SubscriptionSchedule.class)) {
      mockedSchedule
          .when(() -> SubscriptionSchedule.list(any(SubscriptionScheduleListParams.class)))
          .thenReturn(noExistingSchedule);

      subject.scheduleOverageSubscription(
          "customer_id", "metered_price_id", billingCycleAnchor, user);
    }

    verify(subject, times(1))
        .overageScheduleCreation("customer_id", "metered_price_id", billingCycleAnchor);
    verify(userSubscriptionSessionRepositoryMock, times(1))
        .save(any(UserSubscriptionSession.class));
  }

  @Test
  void schedule_overage_subscription_is_skipped_when_already_scheduled() {
    var user = User.builder().id("user_id").build();

    var existingSchedule = someScheduleCollection(someScheduleOn("metered_price_id"));
    try (MockedStatic<SubscriptionSchedule> mockedSchedule =
        mockStatic(SubscriptionSchedule.class)) {
      mockedSchedule
          .when(() -> SubscriptionSchedule.list(any(SubscriptionScheduleListParams.class)))
          .thenReturn(existingSchedule);

      subject.scheduleOverageSubscription("customer_id", "metered_price_id", 123456L, user);
    }

    verify(subject, never()).overageScheduleCreation(anyString(), anyString(), anyLong());
    verify(userSubscriptionSessionRepositoryMock, never()).save(any(UserSubscriptionSession.class));
  }

  private SubscriptionScheduleCollection someScheduleCollection(SubscriptionSchedule... schedules) {
    var collection = new SubscriptionScheduleCollection();
    collection.setData(List.of(schedules));
    return collection;
  }

  private SubscriptionSchedule someScheduleOn(String priceId) {
    var item = new SubscriptionSchedule.Phase.Item();
    item.setPrice(priceId);
    var phase = new SubscriptionSchedule.Phase();
    phase.setItems(List.of(item));
    var schedule = new SubscriptionSchedule();
    schedule.setPhases(List.of(phase));
    return schedule;
  }

  @Test
  void overage_schedule_creation_is_monthly_with_twelve_iterations_then_cancels() {
    var customerId = randomUUID().toString();
    var meteredPriceId = randomUUID().toString();
    var billingCycleAnchor = 123456L;
    var fakeSchedule = new SubscriptionSchedule();
    fakeSchedule.setId(randomUUID().toString());
    try (MockedStatic<SubscriptionSchedule> mockedSchedule =
        mockStatic(SubscriptionSchedule.class)) {
      mockedSchedule
          .when(() -> SubscriptionSchedule.create(any(SubscriptionScheduleCreateParams.class)))
          .thenReturn(fakeSchedule);

      subject.overageScheduleCreation(customerId, meteredPriceId, billingCycleAnchor);

      var captor = ArgumentCaptor.forClass(SubscriptionScheduleCreateParams.class);
      mockedSchedule.verify(() -> SubscriptionSchedule.create(captor.capture()));
      var params = captor.getValue();
      assertEquals(SubscriptionScheduleCreateParams.EndBehavior.CANCEL, params.getEndBehavior());
      var phase = params.getPhases().getFirst();
      assertEquals(12L, phase.getIterations());
      assertEquals(1, phase.getItems().size());
      assertEquals(meteredPriceId, phase.getItems().getFirst().getPrice());
    }
  }

  private void mockToday() {
    when(temporalUtilsMock.today()).thenReturn(LocalDate.of(2025, APRIL, 10));
  }

  private long parisMidnight(LocalDate date) {
    return date.atStartOfDay(ZoneId.of("Europe/Paris")).toEpochSecond();
  }
}
