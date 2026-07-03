package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.UpdateInvoiceStatusRequested;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.service.invoice.InvoiceService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateInvoiceStatusRequestedService implements Consumer<UpdateInvoiceStatusRequested> {
  private final InvoiceService invoiceService;
  private final InvoiceRepository invoiceRepository;

  @Override
  public void accept(UpdateInvoiceStatusRequested updateInvoiceStatusRequested) {
    var invoiceIdentifier = updateInvoiceStatusRequested.getInvoiceIdentifier();
    var invoiceReference = updateInvoiceStatusRequested.getInvoiceReference();
    var userOwnerIdentifier = updateInvoiceStatusRequested.getUserOwnerIdentifier();

    var invoice =
        findInvoiceByReferenceOrIdentifier(
            userOwnerIdentifier, invoiceReference, invoiceIdentifier);

    if (invoice == null) return;

    try {
      invoiceService.crupdateInvoice(
          invoice.toBuilder().status(updateInvoiceStatusRequested.getNewStatus()).build());
    } catch (RuntimeException e) {
      log.error(
          "Error while updating invoice status for user {} and reference {}",
          userOwnerIdentifier,
          invoiceReference,
          e);
      return;
    }
    log.info(
        "Invoice {} status updated to {}",
        invoiceIdentifier,
        updateInvoiceStatusRequested.getNewStatus());
  }

  private @Nullable Invoice findInvoiceByReferenceOrIdentifier(
      String userOwnerIdentifier, String invoiceReference, String invoiceIdentifier) {
    var optionalInvoiceByReference =
        invoiceRepository.findByIdUserAndRef(userOwnerIdentifier, invoiceReference).stream()
            .findAny();
    if (optionalInvoiceByReference.isEmpty()) {
      var optionalInvoiceByIdentifier = invoiceRepository.pwFindOptionalById(invoiceIdentifier);
      if (optionalInvoiceByIdentifier.isEmpty()) {
        log.error(
            "Invoice not found for user {} and reference {}",
            userOwnerIdentifier,
            invoiceReference);
        log.error("Invoice with unique ID {} not found", invoiceIdentifier);
        return null;
      }
      return optionalInvoiceByIdentifier.get();
    }
    return optionalInvoiceByReference.get();
  }
}
