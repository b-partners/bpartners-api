package app.bpartners.api.repository.connectors.account;

import app.bpartners.api.repository.model.AccountConnector;
import java.util.List;

public interface AccountConnectorRepository {
  List<AccountConnector> findByBearer(String bearer);

  AccountConnector findById(String id);

  List<AccountConnector> findByUserId(String userId);

  AccountConnector save(String userId, AccountConnector accountConnector);

  List<AccountConnector> saveAll(String userId, List<AccountConnector> accountConnectors);
}
