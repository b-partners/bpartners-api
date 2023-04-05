package app.bpartners.api.repository;

import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;

public interface BankRepository {
  Bank findById(Long id);

  BankConnection selfUpdateBankConnection();

  BankConnection refreshBankConnection(User user);
}
