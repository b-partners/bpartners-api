package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceUtils {
  private final InvoiceJpaRepository invoiceJpaRepository;

  public boolean hasAvailableReference(
      String accountId, String invoiceId, String reference, InvoiceStatus status) {
    if (reference == null) {
      return true;
    }
    List<HInvoice> actual =
        invoiceJpaRepository.findByIdAccountAndRefAndStatus(accountId, reference, status);
    return actual.isEmpty() || actual.get(0).getId().equals(invoiceId);
  }
}
