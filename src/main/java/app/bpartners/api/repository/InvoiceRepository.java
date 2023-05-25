package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.Invoice;
import java.util.List;

public interface InvoiceRepository {
  Invoice crupdate(Invoice toCrupdate);

  Invoice getById(String invoiceId);

  List<Invoice> findAllByIdUserAndStatus(
      String idUser, InvoiceStatus status, int page, int pageSize);

  List<Invoice> findAllByIdUser(String idUser, int page, int pageSize);

  List<Invoice> saveAll(List<ArchiveInvoice> archiveInvoices);
}
