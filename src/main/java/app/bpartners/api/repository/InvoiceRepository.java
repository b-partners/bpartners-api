package app.bpartners.api.repository;

import app.bpartners.api.model.Invoice;
import java.util.List;

public interface InvoiceRepository {
  Invoice crupdate(Invoice toCrupdate);

  Invoice getById(String invoiceId);

  List<Invoice> findAllByAccountId(String accountId, int page, int pageSize);
}
