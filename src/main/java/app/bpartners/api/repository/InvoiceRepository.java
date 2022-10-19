package app.bpartners.api.repository;

import app.bpartners.api.model.Invoice;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
  Invoice crupdate(Invoice toCrupdate);

  Invoice getById(String invoiceId);

  Optional<Invoice> getOptionalById(String invoiceId);

  List<Invoice> findAllByAccountId(String accountId, int page, int pageSize);
}
