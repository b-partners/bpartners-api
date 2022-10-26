package app.bpartners.api.repository;

import app.bpartners.api.model.InvoiceRelaunch;

public interface InvoiceRelaunchRepository {
  InvoiceRelaunch save(InvoiceRelaunch invoiceRelaunch, String accountId);

  InvoiceRelaunch getByAccountId(String accountId);
}
