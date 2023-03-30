package app.bpartners.api.repository;

import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.UserToken;
import java.time.Instant;

public interface BankRepository {
  Bank findById(Long id);

  BankConnection selfUpdateBankConnection();

  Instant refreshBankConnection(UserToken user);
}
