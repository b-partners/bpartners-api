package app.bpartners.api.repository;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import java.time.Instant;

public interface BankRepository {
  String initiateConnection(User user);

  Bank findByBridgeId(Long id);

  Bank findById(String id);

  BankConnection selfUpdateBankConnection();

  Instant refreshBankConnection(UserToken user);

  boolean disconnectBank(User user);

  String initiateProAccountValidation(UserToken userToken);

  String initiateBankConnectionEdition(Account account);
}
