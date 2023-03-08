package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Invoice;
import java.util.List;

public interface InvoiceRepository {
  Invoice crupdate(Invoice toCrupdate);

  Invoice crupdatePaymentRegulations(String accountId, String invoiceId, InvoiceStatus status,
                                     List<CreatePaymentRegulation> toCrupdate);

  Invoice getById(String invoiceId);

  List<Invoice> findAllByAccountIdAndStatus(
      String accountId, InvoiceStatus status, int page, int pageSize);

  List<Invoice> findAllByAccountId(String accountId, int page, int pageSize);
}
