package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.UpdateInvoiceStatus;
import java.util.List;

public interface InvoiceRepository {
  Invoice crupdate(Invoice toCrupdate);

  List<Invoice> saveAll(List<UpdateInvoiceStatus> invoiceStatuses);

  Invoice getById(String invoiceId);

  List<Invoice> findAllByAccountIdAndStatus(
      String accountId, InvoiceStatus status, int page, int pageSize);

  List<Invoice> findAllByAccountId(String accountId, int page, int pageSize);
}
