package app.bpartners.api.repository;

import app.bpartners.api.model.InvoiceRelaunchConf;

public interface InvoiceRelaunchConfRepository {
  InvoiceRelaunchConf findByInvoiceId(String idInvoice);

  InvoiceRelaunchConf save(InvoiceRelaunchConf invoiceRelaunchConf);
}
