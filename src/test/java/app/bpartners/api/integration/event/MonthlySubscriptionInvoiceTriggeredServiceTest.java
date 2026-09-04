package app.bpartners.api.integration.event;

import static app.bpartners.api.endpoint.event.EventStack.EVENT_STACK_2;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceRequested;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import app.bpartners.api.endpoint.event.model.UpcomingDebitedCustomerExportRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.event.MonthlySubscriptionInvoiceTriggeredService;
import app.bpartners.api.service.subscription.UpcomingDebitedCustomers;
import app.bpartners.api.service.subscription.UpcomingUserDebitService;
import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonthlySubscriptionInvoiceTriggeredServiceTest {
  UserRepository userRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  UserSubscriptionConf userSubscriptionConfMock = mock();
  UpcomingUserDebitService upcomingUserDebitServiceMock = mock();
  MonthlySubscriptionInvoiceTriggeredService subject =
      new MonthlySubscriptionInvoiceTriggeredService(
          userRepositoryMock,
          eventProducerMock,
          userSubscriptionConfMock,
          upcomingUserDebitServiceMock);

  @Test
  void request_monthly_subscription_invoice_for_users_enabled() {
    var userOneMock = mock(User.class);
    var userTwoMock = mock(User.class);
    var userToCreditMock = mock(User.class);
    var userToCreditIdentifier = randomUUID().toString();
    when(upcomingUserDebitServiceMock.getUpcomingDebitedCustomers())
        .thenReturn(new UpcomingDebitedCustomers(List.of(userOneMock, userTwoMock), List.of()));
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditIdentifier);
    when(userRepositoryMock.getById(userToCreditIdentifier)).thenReturn(userToCreditMock);
    var expectedMonthlySubscriptionInvoiceRequestedPage1 =
        MonthlySubscriptionInvoiceRequested.builder()
            .userToCredit(userToCreditMock)
            .userToAttemptDebit(userOneMock)
            .build();
    var expectedMonthlySubscriptionInvoiceRequestedPage2 =
        MonthlySubscriptionInvoiceRequested.builder()
            .userToCredit(userToCreditMock)
            .userToAttemptDebit(userTwoMock)
            .build();

    assertDoesNotThrow(() -> subject.accept(MonthlySubscriptionInvoiceTriggered.builder().build()));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(3)).accept(eventCaptor.capture());
    var upcomingDebitedCustomerExportRequested =
        (UpcomingDebitedCustomerExportRequested) eventCaptor.getAllValues().getFirst().getFirst();
    var monthlySubscriptionInvoiceRequested1 =
        (MonthlySubscriptionInvoiceRequested) eventCaptor.getAllValues().get(1).getFirst();
    var monthlySubscriptionInvoiceRequested2 =
        (MonthlySubscriptionInvoiceRequested) eventCaptor.getAllValues().getLast().getFirst();

    assertEquals(
        new UpcomingDebitedCustomerExportRequested(YearMonth.now().minusMonths(1)),
        upcomingDebitedCustomerExportRequested);
    assertEquals(
        Duration.ofSeconds(120L), upcomingDebitedCustomerExportRequested.maxConsumerDuration());
    assertEquals(
        Duration.ofSeconds(60),
        upcomingDebitedCustomerExportRequested.maxConsumerBackoffBetweenRetries());
    assertEquals(EVENT_STACK_2, upcomingDebitedCustomerExportRequested.getEventStack());
    assertEquals(
        expectedMonthlySubscriptionInvoiceRequestedPage1, monthlySubscriptionInvoiceRequested1);
    assertEquals(
        expectedMonthlySubscriptionInvoiceRequestedPage2, monthlySubscriptionInvoiceRequested2);
    assertEquals(
        Duration.ofSeconds(600L), monthlySubscriptionInvoiceRequested1.maxConsumerDuration());
    assertEquals(
        Duration.ofSeconds(60L),
        monthlySubscriptionInvoiceRequested1.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void does_not_produce_any_event_when_no_upcoming_debited_customer() {
    when(upcomingUserDebitServiceMock.getUpcomingDebitedCustomers())
        .thenReturn(new UpcomingDebitedCustomers(List.of(), List.of()));

    assertDoesNotThrow(() -> subject.accept(MonthlySubscriptionInvoiceTriggered.builder().build()));

    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void exports_even_when_only_not_billed_customers_are_present() {
    var notBilledStripeCustomerMock = mock(com.stripe.model.Customer.class);
    var userToCreditMock = mock(User.class);
    var userToCreditIdentifier = randomUUID().toString();
    when(upcomingUserDebitServiceMock.getUpcomingDebitedCustomers())
        .thenReturn(new UpcomingDebitedCustomers(List.of(), List.of(notBilledStripeCustomerMock)));
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditIdentifier);
    when(userRepositoryMock.getById(userToCreditIdentifier)).thenReturn(userToCreditMock);

    assertDoesNotThrow(() -> subject.accept(MonthlySubscriptionInvoiceTriggered.builder().build()));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    // Only the export event : no billedUsers user means no MonthlySubscriptionInvoiceRequested.
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    assertEquals(
        new UpcomingDebitedCustomerExportRequested(YearMonth.now().minusMonths(1)),
        eventCaptor.getValue().getFirst());
  }
}
