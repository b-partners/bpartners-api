package app.bpartners.api.integration.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceRequested;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.event.MonthlySubscriptionInvoiceTriggeredService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonthlySubscriptionInvoiceTriggeredServiceTest {
  UserRepository userRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  UserSubscriptionConf userSubscriptionConfMock = mock();
  MonthlySubscriptionInvoiceTriggeredService subject =
      new MonthlySubscriptionInvoiceTriggeredService(
          userRepositoryMock, eventProducerMock, userSubscriptionConfMock);

  @Test
  void request_monthly_subscription_invoice_for_users_enabled() {
    var userOneMock = mock(User.class);
    var userTwoMock = mock(User.class);
    var userToCreditMock = mock(User.class);
    var userToCreditIdentifier = randomUUID().toString();
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(List.of(userOneMock, userTwoMock));
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
    verify(eventProducerMock, times(2)).accept(eventCaptor.capture());
    var monthlySubscriptionInvoiceRequested1 =
        (MonthlySubscriptionInvoiceRequested) eventCaptor.getAllValues().getFirst().getFirst();
    var monthlySubscriptionInvoiceRequested2 =
        (MonthlySubscriptionInvoiceRequested) eventCaptor.getAllValues().getLast().getFirst();

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
