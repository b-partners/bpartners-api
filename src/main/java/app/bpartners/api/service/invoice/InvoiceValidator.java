package app.bpartners.api.service.invoice;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.InvoiceRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceValidator {
  private final InvoiceRepository invoiceRepository;

  public void checkReferenceAvailability(Invoice actual) {
    if (!hasReferenceAvailable(actual)) {
      throw new BadRequestException(
          "La référence " + actual.getRealReference() + " est déjà utilisée");
    }
  }

  private boolean hasReferenceAvailable(Invoice actual) {
    var reference = actual.getRealReference();
    if (reference == null) {
      return true;
    }
    var invoices = invoiceRepository.findByIdUserAndRef(actual.getUser().getId(), reference);
    if (isUniqueConfirmedInvoice(invoices, actual)
        || isMatchingConfirmedInvoice(invoices, actual)) {
      return true;
    }
    return evaluateInvoiceStatus(invoices, actual);
  }

  private boolean isUniqueConfirmedInvoice(List<Invoice> invoices, Invoice actual) {
    return invoices.size() == 1
        && invoices.getFirst().getStatus() == CONFIRMED
        && actual.getStatus() == CONFIRMED
        && actual.getId().equals(invoices.getFirst().getId());
  }

  private boolean isMatchingConfirmedInvoice(List<Invoice> invoices, Invoice actual) {
    return actual.getStatus() == CONFIRMED
        && invoices.stream()
            .anyMatch(
                invoice ->
                    invoice.getStatus() == CONFIRMED && invoice.getId().equals(actual.getId()));
  }

  private boolean evaluateInvoiceStatus(List<Invoice> invoices, Invoice actual) {
    var status = actual.getStatus();
    boolean isToBeConfirmed =
        invoices.isEmpty()
            || invoices.stream().anyMatch(invoice -> invoice.getStatus() == PROPOSAL);
    boolean isToBePaid = invoices.stream().anyMatch(invoice -> invoice.getStatus() == CONFIRMED);
    if (status != CONFIRMED && status != PAID) {
      return invoices.isEmpty()
          || invoices.stream().anyMatch(invoice -> invoice.getId().equals(actual.getId()));
    } else {
      return status == CONFIRMED ? isToBeConfirmed : isToBePaid;
    }
  }
}
