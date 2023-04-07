package app.bpartners.api.repository;

import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.UserToken;
import java.time.Instant;
import java.util.Optional;

public interface BankRepository {
  Bank findByBridgeId(Long id);

  Bank findById(String id);

  BankConnection selfUpdateBankConnection();

  Instant refreshBankConnection(UserToken user);
}
