package app.bpartners.api.repository;

import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import java.time.Instant;
import org.springframework.web.servlet.view.RedirectView;

public interface BankRepository {
  String initiateConnection(User user);

  Bank findByBridgeId(Long id);

  Bank findById(String id);

  BankConnection selfUpdateBankConnection();

  Instant refreshBankConnection(UserToken user);

  boolean disconnectBank(User user);

  RedirectView validateProItems();

  RedirectView editItems();
}
