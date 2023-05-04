package app.bpartners.api.repository;

import app.bpartners.api.model.UserInvoiceRelaunchConf;

public interface UserInvoiceRelaunchConfRepository {
  UserInvoiceRelaunchConf save(String idUser, UserInvoiceRelaunchConf relaunchConf);

  UserInvoiceRelaunchConf getByIdUser(String idUser);
}
