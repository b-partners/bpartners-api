package app.bpartners.api.repository.bridge.repository;

import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import java.util.List;

public interface BridgeAccountRepository {
  List<BridgeAccount> findByBearer(String bearer);

  List<BridgeAccount> findAllByAuthenticatedUser();

  BridgeAccount findById(String id);
}
