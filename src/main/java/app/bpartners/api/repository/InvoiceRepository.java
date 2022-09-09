package app.bpartners.api.repository;

import app.bpartners.api.model.Invoice;

public interface InvoiceRepository {
  Invoice crupdate(Invoice toCrupdate);

  Invoice getById(String invoiceId);
}
