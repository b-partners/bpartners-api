package app.bpartners.api.repository;

import app.bpartners.api.model.InvoiceRelaunch;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface InvoiceRelaunchRepository {
  List<InvoiceRelaunch> getInvoiceRelaunchesByInvoiceIdAndCriteria(
      String invoiceId,
      Boolean isUserRelaunched,
      Pageable pageable);

  InvoiceRelaunch save(String invoiceId);
}
