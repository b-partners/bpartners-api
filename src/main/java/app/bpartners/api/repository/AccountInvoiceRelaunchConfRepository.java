package app.bpartners.api.repository;

import app.bpartners.api.model.AccountInvoiceRelaunchConf;

public interface AccountInvoiceRelaunchConfRepository {
  AccountInvoiceRelaunchConf save(
      AccountInvoiceRelaunchConf accountInvoiceRelaunchConf,
      String accountId
  );

  AccountInvoiceRelaunchConf getByAccountId(String accountId);
}
