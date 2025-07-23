package app.bpartners.api.repository;

import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;

public interface BankRepository {
  Bank findByExternalId(String id);

  BankConnection updateBankConnection(User user);
}
