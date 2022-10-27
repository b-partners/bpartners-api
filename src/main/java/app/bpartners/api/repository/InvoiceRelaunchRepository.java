package app.bpartners.api.repository;

import app.bpartners.api.model.InvoiceRelaunch;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface InvoiceRelaunchRepository {
  List<InvoiceRelaunch> getInvoiceRelaunchesByInvoiceId(String invoiceId, Pageable pageable);

  InvoiceRelaunch save(String invoiceId);
}
