package app.bpartners.api.repository;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface InvoiceRelaunchRepository {
  List<InvoiceRelaunch> getByInvoiceId(String invoiceId, String type, Pageable pageable);

  InvoiceRelaunch save(Invoice invoice, String object, String htmlBody, boolean isUserRelaunched);
}
