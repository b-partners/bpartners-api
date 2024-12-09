package app.bpartners.api.integration.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceRequested;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.event.MonthlySubscriptionInvoiceTriggeredService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonthlySubscriptionInvoiceTriggeredServiceTest {
  UserRepository userRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  MonthlySubscriptionInvoiceTriggeredService subject =
      new MonthlySubscriptionInvoiceTriggeredService(userRepositoryMock, eventProducerMock);

  @Test
  void request_monthly_subscription_invoice_creation_under_500_users_enabled() {
    var expectedUserCount = 100L;
    when(userRepositoryMock.countUsersByStatus(EnableStatus.ENABLED)).thenReturn(expectedUserCount);
    var expectedMonthlySubscriptionInvoiceRequested =
        MonthlySubscriptionInvoiceRequested.builder().userPage(1L).build();

    assertDoesNotThrow(() -> subject.accept(MonthlySubscriptionInvoiceTriggered.builder().build()));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    var monthlySubscriptionInvoiceRequested =
        (MonthlySubscriptionInvoiceRequested) eventCaptor.getValue().getFirst();

    assertEquals(expectedMonthlySubscriptionInvoiceRequested, monthlySubscriptionInvoiceRequested);
  }

  @Test
  void request_monthly_subscription_invoice_creation_under_1000_users_enabled() {
    var expectedUserCount = 1000L;
    when(userRepositoryMock.countUsersByStatus(EnableStatus.ENABLED)).thenReturn(expectedUserCount);
    var expectedMonthlySubscriptionInvoiceRequestedPage1 =
        MonthlySubscriptionInvoiceRequested.builder().userPage(1L).build();
    var expectedMonthlySubscriptionInvoiceRequestedPage2 =
        MonthlySubscriptionInvoiceRequested.builder().userPage(2L).build();

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
    var expectedUserCount = 0L;
    when(userRepositoryMock.countUsersByStatus(EnableStatus.ENABLED)).thenReturn(expectedUserCount);

    assertDoesNotThrow(() -> subject.accept(MonthlySubscriptionInvoiceTriggered.builder().build()));

    verify(eventProducerMock, never()).accept(any());
  }
}
