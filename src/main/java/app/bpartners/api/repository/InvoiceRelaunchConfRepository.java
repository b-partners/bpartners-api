package app.bpartners.api.repository;

import app.bpartners.api.model.InvoiceRelaunchConf;

public interface InvoiceRelaunchConfRepository {
  InvoiceRelaunchConf save(InvoiceRelaunchConf invoiceRelaunchConf, String accountId);

  InvoiceRelaunchConf getByAccountId(String accountId);
}
