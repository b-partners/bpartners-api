package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import java.util.List;

public interface InvoiceRepository {
  Invoice crupdate(Invoice toCrupdate);

  Invoice crupdateInvoiceProducts(String accountId, String invoiceId, InvoiceStatus status,
                                  List<InvoiceProduct> products);

  Invoice getById(String invoiceId);

  List<Invoice> findAllByAccountIdAndStatus(
      String accountId, InvoiceStatus status, int page, int pageSize);

  List<Invoice> findAllByAccountId(String accountId, int page, int pageSize);
}
