package app.bpartners.api.repository;

import app.bpartners.api.repository.model.AccountConnector;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CachedAccountConnectorRepository implements AccountConnectorRepository {
  private final AccountConnectorRepository toCache;

  @Override
  public List<AccountConnector> findByBearer(String bearer) {
    return saveAll(toCache.findByBearer(bearer));
  }

  @Override
  public AccountConnector findById(String id) {
    return save(toCache.findById(id));
  }

  @Override
  public List<AccountConnector> findByUserId(String userId) {
    return saveAll(toCache.findByUserId(userId));
  }

  @Override
  public AccountConnector save(AccountConnector accountConnector) {
    return toCache.save(accountConnector);
  }

  @Override
  public List<AccountConnector> saveAll(List<AccountConnector> accountConnectors) {
    return toCache.saveAll(accountConnectors);
  }
}
