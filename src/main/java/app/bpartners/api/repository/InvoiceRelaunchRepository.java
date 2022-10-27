package app.bpartners.api.repository;

import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.repository.jpa.model.HInvoice;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface InvoiceRelaunchRepository {
  List<InvoiceRelaunch> getInvoiceRelaunchesByInvoiceId(
      String invoiceId, String type, Pageable pageable);

  InvoiceRelaunch save(HInvoice invoice);
}
