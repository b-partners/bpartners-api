package app.bpartners.api.repository;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import java.time.Instant;

public interface BankRepository {
  String initiateConnection(User user);

  Bank findByExternalId(String id);

  Bank findById(String id);

  BankConnection selfUpdateBankConnection();

  Instant refreshBankConnection(UserToken user);

  String initiateScaSync(Account account);

  boolean disconnectBank(User user);

  String initiateProValidation(String accountId);

  String initiateBankConnectionEdition(Account account);
}
