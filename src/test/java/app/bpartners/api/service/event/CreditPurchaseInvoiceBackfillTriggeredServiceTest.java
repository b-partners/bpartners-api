package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.event.EventStack.EVENT_STACK_2;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.CreditOperationInvoiceRequested;
import app.bpartners.api.endpoint.event.model.CreditPurchaseInvoiceBackfillTriggered;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreditPurchaseInvoiceBackfillTriggeredServiceTest {
  CreditPurchaseRepository creditPurchaseRepository = mock();
  CreditTransactionRepository creditTransactionRepository = mock();
  EventProducer eventProducer = mock();
  CreditPurchaseInvoiceBackfillTriggeredService subject =
      new CreditPurchaseInvoiceBackfillTriggeredService(
          creditPurchaseRepository, creditTransactionRepository, eventProducer);

  @Test
  void requests_an_invoice_for_every_completed_purchase_without_one() {
    givenNotInvoicedPurchases(somePurchase("purchase_1"), somePurchase("purchase_2"));
    givenTransaction("purchase_1", "tx_1");
    givenTransaction("purchase_2", "tx_2");

    subject.accept(new CreditPurchaseInvoiceBackfillTriggered());

    var requested = requestedTransactionIds();
    assertEquals(List.of("tx_1", "tx_2"), requested);
  }

  @Test
  void reads_only_completed_purchases_without_invoice() {
    givenNotInvoicedPurchases();

    subject.accept(new CreditPurchaseInvoiceBackfillTriggered());

    verify(creditPurchaseRepository).findByStatusAndInvoiceIdIsNull(COMPLETED);
  }

  @Test
  void requests_nothing_when_every_purchase_is_already_invoiced() {
    givenNotInvoicedPurchases();

    subject.accept(new CreditPurchaseInvoiceBackfillTriggered());

    verify(eventProducer, never()).accept(any());
  }

  @Test
  void skips_a_purchase_whose_credit_transaction_is_missing() {
    givenNotInvoicedPurchases(somePurchase("purchase_1"), somePurchase("purchase_2"));
    when(creditTransactionRepository.findFirstByCreditPurchaseId("purchase_1"))
        .thenReturn(Optional.empty());
    givenTransaction("purchase_2", "tx_2");

    subject.accept(new CreditPurchaseInvoiceBackfillTriggered());

    assertEquals(List.of("tx_2"), requestedTransactionIds());
  }

  @Test
  void requests_nothing_when_no_purchase_has_a_credit_transaction() {
    givenNotInvoicedPurchases(somePurchase("purchase_1"));
    when(creditTransactionRepository.findFirstByCreditPurchaseId("purchase_1"))
        .thenReturn(Optional.empty());

    subject.accept(new CreditPurchaseInvoiceBackfillTriggered());

    verify(eventProducer, never()).accept(any());
  }

  @Test
  void retries_the_backfill_for_at_most_ten_minutes() {
    var event = new CreditPurchaseInvoiceBackfillTriggered();

    assertEquals(Duration.ofMinutes(10L), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), event.maxConsumerBackoffBetweenRetries());
    assertEquals(EVENT_STACK_2, event.getEventStack());
  }

  private List<String> requestedTransactionIds() {
    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(eventsCaptor.capture());
    return eventsCaptor.getValue().stream()
        .map(event -> ((CreditOperationInvoiceRequested) event).getCreditTransaction().getId())
        .toList();
  }

  private void givenNotInvoicedPurchases(CreditPurchase... purchases) {
    when(creditPurchaseRepository.findByStatusAndInvoiceIdIsNull(COMPLETED))
        .thenReturn(List.of(purchases));
  }

  private void givenTransaction(String purchaseId, String transactionId) {
    when(creditTransactionRepository.findFirstByCreditPurchaseId(purchaseId))
        .thenReturn(Optional.of(CreditTransaction.builder().id(transactionId).build()));
  }

  private CreditPurchase somePurchase(String id) {
    return CreditPurchase.builder().id(id).userId("buyer_id").status(COMPLETED).build();
  }
}
