package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static java.time.Month.APRIL;
import static java.time.Month.MAY;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.Account;
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
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionScheduleCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.LocalDate;
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
    when(product.getPriceInCentsWithVat()).thenReturn(5880L);
    when(product.getType()).thenReturn(MONTHLY);
    com.stripe.model.SubscriptionSchedule fakeSchedule =
        new com.stripe.model.SubscriptionSchedule();
    fakeSchedule.setId("schedule_id");

    doReturn(fakeSchedule)
        .when(subject)
        .subscriptionScheduleCreation(anyString(), any(Subscription.class), anyString(), anyLong());

    subject.scheduleSubscription(customer, subscription, price, billingCycleAnchor, user);

    verify(userSubscriptionSessionRepositoryMock, times(1))
        .save(any(UserSubscriptionSession.class));
  }

  @Test
  void subscription_schedule_creation() {
    var customerId = randomUUID().toString();
    var billingCycleAnchor = 123456L;
    var subscriptionProduct = mock(SubscriptionProduct.class);
    when(subscriptionProduct.getE2Id()).thenReturn("e2id");
    when(subscriptionProduct.getPriceInCentsWithVat()).thenReturn(5880L);
    when(subscriptionProduct.getType()).thenReturn(MONTHLY);
    var subscription = mock(Subscription.class);
    when(subscription.getSubscriptionProduct()).thenReturn(subscriptionProduct);
    var meteredPriceId = randomUUID().toString();
    var fakeScheduleIdentifier = randomUUID().toString();
    var fakeSchedule = new SubscriptionSchedule();
    fakeSchedule.setId(fakeScheduleIdentifier);
    try (MockedStatic<SubscriptionSchedule> mockedSchedule =
        mockStatic(SubscriptionSchedule.class)) {
      mockedSchedule
          .when(() -> SubscriptionSchedule.create(any(SubscriptionScheduleCreateParams.class)))
          .thenReturn(fakeSchedule);

      var actual =
          subject.subscriptionScheduleCreation(
              customerId, subscription, meteredPriceId, billingCycleAnchor);

      assertThat(actual.getId()).isEqualTo(fakeScheduleIdentifier);
      mockedSchedule.verify(
          () -> SubscriptionSchedule.create(any(SubscriptionScheduleCreateParams.class)), times(1));
    }
  }

  @Test
  void create_session_if_trial_end_is_between_1_to_4_actual_month() throws StripeException {
    var trialEnd = LocalDate.of(2025, APRIL, 2);
    mockTemporalWindow();
    var customer = mock(Customer.class);
    var product = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    var accountIdentifier = randomUUID().toString();
    var userIdentifier = randomUUID().toString();
    var user =
        User.builder()
            .id(userIdentifier)
            .accounts(List.of(Account.builder().id(accountIdentifier).build()))
            .build();

    doNothing()
        .when(subject)
        .scheduleSubscription(customer, subscription, price, billingCycleAnchor, user);

    var actual =
        subject.initiateSubscriptionWorkflow(
            user, trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(subject, times(1))
        .scheduleSubscription(customer, subscription, price, billingCycleAnchor, user);
    verify(subject, never()).createSessionSubscription(any(), any(), any(), any(), any());
    assertThat(actual.getRedirectionUrl())
        .isEqualTo(
            "https://dashboard.birdia.fr/account/"
                + user.getDefaultAccount().getId()
                + "?stripeStatus=done");
  }

  @Test
  void create_session_if_trial_end_is_before_end_of_actual_month_not_between_1_and_4_of_month()
      throws StripeException {
    var trialEnd = LocalDate.of(2025, APRIL, 10);
    var userIdentifier = randomUUID().toString();
    mockTemporalWindow();
    when(temporalUtilsMock.endOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 30));
    var mockSession = mock(Session.class);
    var customer = mock(Customer.class);
    var product = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var urls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    var billingCycleAnchor = 123456L;
    var user = User.builder().id(userIdentifier).build();
    doReturn(mockSession)
        .when(subject)
        .createSessionSubscription(customer, subscription, price, urls, billingCycleAnchor);

    var actual =
        subject.initiateSubscriptionWorkflow(
            user, trialEnd, customer, product, price, urls, billingCycleAnchor, subscription);

    verify(subject, times(1))
        .createSessionSubscription(customer, subscription, price, urls, billingCycleAnchor);
    verify(subject, never()).scheduleSubscription(any(), any(), any(), any(), any());
    assertEquals(urls, actual.getRedirectionStatusUrls());
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
    var accountIdentifier = randomUUID().toString();
    var user =
        User.builder().accounts(List.of(Account.builder().id(accountIdentifier).build())).build();
    doNothing()
        .when(subject)
        .scheduleSubscription(customer, subscription, price, billingCycleAnchor, user);

    assertDoesNotThrow(
        () ->
            subject.initiateSubscriptionWorkflow(
                user, trialEnd, customer, product, price, urls, billingCycleAnchor, subscription));

    verify(subject, times(1)).scheduleSubscription(any(), any(), any(), any(), any());
    verify(subject, never()).createSessionSubscription(any(), any(), any(), any(), any());
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
    var accountIdentifier = randomUUID().toString();
    var userIdentifier = randomUUID().toString();
    var user =
        User.builder()
            .id(userIdentifier)
            .accounts(List.of(Account.builder().id(accountIdentifier).build()))
            .build();
    doNothing()
        .when(subject)
        .scheduleSubscription(customer, subscription, price, billingCycleAnchor, user);

    assertDoesNotThrow(
        () ->
            subject.initiateSubscriptionWorkflow(
                user, trialEnd, customer, product, price, urls, billingCycleAnchor, subscription));

    verify(subject, times(1))
        .scheduleSubscription(customer, subscription, price, billingCycleAnchor, user);
    verify(subject, never()).createSessionSubscription(any(), any(), any(), any(), any());
  }

  @Test
  void subscription_schedule_creation_yearly_uses_annual_price_and_no_metered() {
    var customerId = randomUUID().toString();
    var billingCycleAnchor = 123456L;
    var subscriptionProduct = mock(SubscriptionProduct.class);
    when(subscriptionProduct.getAnnualE2PriceId()).thenReturn("annual_price_id");
    var subscription = mock(Subscription.class);
    when(subscription.getSubscriptionProduct()).thenReturn(subscriptionProduct);
    when(subscription.getBillingInterval()).thenReturn(BillingInterval.YEARLY);
    var fakeSchedule = new SubscriptionSchedule();
    fakeSchedule.setId(randomUUID().toString());
    try (MockedStatic<SubscriptionSchedule> mockedSchedule =
        mockStatic(SubscriptionSchedule.class)) {
      mockedSchedule
          .when(() -> SubscriptionSchedule.create(any(SubscriptionScheduleCreateParams.class)))
          .thenReturn(fakeSchedule);

      subject.subscriptionScheduleCreation(
          customerId, subscription, randomUUID().toString(), billingCycleAnchor);

      var captor = ArgumentCaptor.forClass(SubscriptionScheduleCreateParams.class);
      mockedSchedule.verify(() -> SubscriptionSchedule.create(captor.capture()));
      var items = captor.getValue().getPhases().getFirst().getItems();
      assertEquals(1, items.size());
      assertEquals("annual_price_id", items.getFirst().getPrice());
      assertNull(items.getFirst().getPriceData());
    }
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

  @Test
  void schedule_subscription_yearly_creates_base_and_overage_schedules() {
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn("customer_id");
    var subscription = mock(Subscription.class);
    when(subscription.getBillingInterval()).thenReturn(BillingInterval.YEARLY);
    var price = mock(Price.class);
    when(price.getId()).thenReturn("metered_price_id");
    var billingCycleAnchor = 123456L;
    var user = User.builder().id("user_id").build();
    var baseSchedule = new SubscriptionSchedule();
    baseSchedule.setId("base_schedule_id");
    var overageSchedule = new SubscriptionSchedule();
    overageSchedule.setId("overage_schedule_id");
    doReturn(baseSchedule)
        .when(subject)
        .subscriptionScheduleCreation(anyString(), any(Subscription.class), anyString(), anyLong());
    doReturn(overageSchedule)
        .when(subject)
        .overageScheduleCreation(anyString(), anyString(), anyLong());

    subject.scheduleSubscription(customer, subscription, price, billingCycleAnchor, user);

    verify(subject, times(1)).overageScheduleCreation("customer_id", "metered_price_id", 123456L);
    verify(userSubscriptionSessionRepositoryMock, times(2))
        .save(any(UserSubscriptionSession.class));
  }

  @Test
  void schedule_subscription_monthly_does_not_create_overage_schedule() {
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn("customer_id");
    var subscription = mock(Subscription.class);
    when(subscription.getBillingInterval()).thenReturn(BillingInterval.MONTHLY);
    var price = mock(Price.class);
    when(price.getId()).thenReturn("metered_price_id");
    var billingCycleAnchor = 123456L;
    var user = User.builder().id("user_id").build();
    var baseSchedule = new SubscriptionSchedule();
    baseSchedule.setId("base_schedule_id");
    doReturn(baseSchedule)
        .when(subject)
        .subscriptionScheduleCreation(anyString(), any(Subscription.class), anyString(), anyLong());

    subject.scheduleSubscription(customer, subscription, price, billingCycleAnchor, user);

    verify(subject, never()).overageScheduleCreation(anyString(), anyString(), anyLong());
    verify(userSubscriptionSessionRepositoryMock, times(1))
        .save(any(UserSubscriptionSession.class));
  }

  @Test
  void create_session_subscription_yearly_uses_annual_price_and_no_metered_line_item()
      throws StripeException {
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
    var billingCycleAnchor = 123456L;
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

  private void mockTemporalWindow() {
    // Setup mocks for trialEnd checks
    when(temporalUtilsMock.startOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 1));
    when(temporalUtilsMock.fifthOfActualMonth()).thenReturn(LocalDate.of(2025, APRIL, 4));

    when(temporalUtilsMock.startOfNextMonth()).thenReturn(LocalDate.of(2025, MAY, 1));
    when(temporalUtilsMock.fifthOfNextMonth()).thenReturn(LocalDate.of(2025, MAY, 4));
  }
}
