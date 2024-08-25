package app.bpartners.api.repository;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.bank.BankConnection;
import java.time.Instant;
import java.util.List;

public interface BankRepository {
  String initiateConnection(User user);

  Bank findByExternalId(String id);

  Bank findById(String id);

  BankConnection updateBankConnection(User user);

  Instant refreshBankConnection(UserToken user);

  String initiateScaSync(Account account);

  boolean disconnectBank(User user);

  String initiateProValidation(String accountId);

  String initiateBankConnectionEdition(Account account);

  List<BankConnection> getAllConnectionByUser(User user);
}
