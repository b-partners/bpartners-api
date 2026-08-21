package app.bpartners.api.service.event;

import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.CreditOperationInvoiceRequested;
import app.bpartners.api.endpoint.event.model.CreditPurchaseInvoiceBackfillTriggered;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditPurchaseInvoiceBackfillTriggeredService
    implements Consumer<CreditPurchaseInvoiceBackfillTriggered> {
  private final CreditPurchaseRepository creditPurchaseRepository;
  private final CreditTransactionRepository creditTransactionRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(CreditPurchaseInvoiceBackfillTriggered event) {
    var notInvoicedPurchases = creditPurchaseRepository.findByStatusAndInvoiceIdIsNull(COMPLETED);
    log.info(
        "Credit purchase invoice backfill triggered, {} completed purchase(s) without invoice",
        notInvoicedPurchases.size());
    var requests =
        notInvoicedPurchases.stream()
            .map(this::creditTransactionOf)
            .flatMap(Optional::stream)
            .map(
                creditTransaction ->
                    CreditOperationInvoiceRequested.builder()
                        .creditTransaction(creditTransaction)
                        .build())
            .toList();
    if (requests.isEmpty()) {
      log.info("No credit purchase invoice to backfill");
      return;
    }
    eventProducer.accept(requests);
    log.info("Requested {} credit purchase invoice(s) for backfill", requests.size());
  }

  private Optional<CreditTransaction> creditTransactionOf(CreditPurchase creditPurchase) {
    var creditTransaction =
        creditTransactionRepository.findFirstByCreditPurchaseId(creditPurchase.getId());
    if (creditTransaction.isEmpty()) {
      log.warn(
          "CreditPurchase.id={} is completed without any CreditTransaction, skipping its invoice"
              + " backfill",
          creditPurchase.getId());
    }
    return creditTransaction;
  }
}
