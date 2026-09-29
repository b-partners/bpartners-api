package app.bpartners.api.service.event;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.SubscriptionRemainingPeriodInvoiceRequested;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionRemainingPeriodInvoiceRequestedServiceTest {
  private static final String USER_ID = "subscriber_id";

  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();
  SubscriptionPaymentInvoiceRequestedService subscriptionPaymentInvoiceService = mock();
  SubscriptionRemainingPeriodInvoiceRequestedService subject =
      new SubscriptionRemainingPeriodInvoiceRequestedService(
          subscriptionPaymentRepository, subscriptionPaymentInvoiceService);

  @Test
  void invoices_the_remaining_period_from_the_latest_invoiced_monthly_payment() {
    when(subscriptionPaymentRepository.findByUserIdAndInvoiceIdIsNotNull(USER_ID))
        .thenReturn(
            List.of(
                invoicedPayment("september_payment_id", "2026-09-01T09:30:00Z", MONTHLY),
                invoicedPayment("august_payment_id", "2026-08-01T09:30:00Z", MONTHLY)));
    when(subscriptionPaymentInvoiceService.invoiceRemainingCommitmentPeriod(any()))
        .thenReturn(Optional.of(Invoice.builder().id("invoice_id").ref("REF").build()));

    subject.accept(someEvent());

    var captor = ArgumentCaptor.forClass(SubscriptionPayment.class);
    verify(subscriptionPaymentInvoiceService).invoiceRemainingCommitmentPeriod(captor.capture());
    assertEquals("september_payment_id", captor.getValue().getId());
  }

  @Test
  void ignores_refunded_and_yearly_payments() {
    when(subscriptionPaymentRepository.findByUserIdAndInvoiceIdIsNotNull(USER_ID))
        .thenReturn(
            List.of(
                invoicedPayment("yearly_payment_id", "2026-10-01T09:30:00Z", YEARLY),
                invoicedPayment("refunded_payment_id", "2026-09-01T09:30:00Z", MONTHLY).toBuilder()
                    .refundedDatetime(Instant.parse("2026-09-20T09:30:00Z"))
                    .build(),
                invoicedPayment("august_payment_id", "2026-08-01T09:30:00Z", MONTHLY)));
    when(subscriptionPaymentInvoiceService.invoiceRemainingCommitmentPeriod(any()))
        .thenReturn(Optional.empty());

    subject.accept(someEvent());

    var captor = ArgumentCaptor.forClass(SubscriptionPayment.class);
    verify(subscriptionPaymentInvoiceService).invoiceRemainingCommitmentPeriod(captor.capture());
    assertEquals("august_payment_id", captor.getValue().getId());
  }

  @Test
  void invoices_nothing_when_the_user_has_no_invoiced_monthly_payment() {
    when(subscriptionPaymentRepository.findByUserIdAndInvoiceIdIsNotNull(USER_ID))
        .thenReturn(List.of(invoicedPayment("yearly_payment_id", "2026-09-01T09:30:00Z", YEARLY)));

    subject.accept(someEvent());

    verify(subscriptionPaymentInvoiceService, never()).invoiceRemainingCommitmentPeriod(any());
  }

  @Test
  void retries_the_invoicing_for_at_most_five_minutes() {
    var event = someEvent();

    assertEquals(Duration.ofMinutes(5L), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), event.maxConsumerBackoffBetweenRetries());
  }

  private SubscriptionRemainingPeriodInvoiceRequested someEvent() {
    return SubscriptionRemainingPeriodInvoiceRequested.builder().userId(USER_ID).build();
  }

  private SubscriptionPayment invoicedPayment(
      String paymentId, String periodStart, BillingInterval billingInterval) {
    return SubscriptionPayment.builder()
        .id(paymentId)
        .userId(USER_ID)
        .invoiceId(paymentId + "_invoice_id")
        .billingInterval(billingInterval)
        .periodStartDatetime(Instant.parse(periodStart))
        .paymentDatetime(Instant.parse(periodStart))
        .build();
  }
}
