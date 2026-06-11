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
    when(upcomingUserDebitServiceMock.getUpcomingUserDebited())
        .thenReturn(List.of(userOneMock, userTwoMock));
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
        new UpcomingDebitedCustomerExportRequested(YearMonth.now().minusMonths(1L)),
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
  void request_monthly_subscription_invoice_creation_with_any_users() {
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(List.of());

    assertDoesNotThrow(() -> subject.accept(MonthlySubscriptionInvoiceTriggered.builder().build()));

    verify(eventProducerMock, never()).accept(any());
  }
}
