package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.repository.model.AccountConnector;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CachedAccountConnectorRepository implements AccountConnectorRepository {
  private final AccountConnectorRepository toCache;

  @Override
  public List<AccountConnector> findByBearer(String bearer) {
    return saveAll(AuthProvider.getAuthenticatedUserId(), toCache.findByBearer(bearer));
  }

  @Override
  public AccountConnector findById(String id) {
    return save(null, toCache.findById(id));
  }

  @Override
  public List<AccountConnector> findByUserId(String userId) {
    return saveAll(userId, toCache.findByUserId(userId));
  }

  @Override
  public AccountConnector save(String idUser, AccountConnector accountConnector) {
    return toCache.save(idUser, accountConnector);
  }

  @Override
  public List<AccountConnector> saveAll(String idUser, List<AccountConnector> accountConnectors) {
    return toCache.saveAll(idUser, accountConnectors);
  }
}
