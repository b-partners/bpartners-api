package app.bpartners.api.repository;

import app.bpartners.api.repository.model.AccountConnector;
import java.util.List;

public interface AccountConnectorRepository {
  List<AccountConnector> findByBearer(String bearer);

  AccountConnector findById(String id);

  List<AccountConnector> findByUserId(String userId);

  AccountConnector save(AccountConnector accountConnector);

  List<AccountConnector> saveAll(List<AccountConnector> accountConnectors);
}
