package app.bpartners.api.repository;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;

public interface BankRepository {
  String initiateConnection(User user);

  Bank findByExternalId(String id);

  Bank findById(String id);

  BankConnection updateBankConnection(User user);

  boolean disconnectBank(User user);

}
