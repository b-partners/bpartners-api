package app.bpartners.api.repository;

import app.bpartners.api.repository.bridge.repository.implementation.BridgeAccountConnectorRepository;
import app.bpartners.api.repository.model.AccountConnector;
import app.bpartners.api.repository.swan.implementation.SwanAccountConnectorRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.AuthProvider.getAuthenticatedUserId;

@Repository
@Primary
@AllArgsConstructor
public class AccountConnectorRepositoryFacade implements AccountConnectorRepository {
  private final SwanAccountConnectorRepository swanConnector;
  private final BridgeAccountConnectorRepository bridgeConnector;

  private AccountConnectorRepository getAccountConnectorDependentRepository() {
    var swanAccounts = swanConnector.findByUserId(getAuthenticatedUserId());
    return new CachedAccountConnectorRepository(
        swanAccounts.isEmpty() ? bridgeConnector : swanConnector);
  }

  @Override
  public List<AccountConnector> findByBearer(String bearer) {
    return getAccountConnectorDependentRepository().findByBearer(bearer);
  }

  @Override
  public AccountConnector findById(String id) {
    return getAccountConnectorDependentRepository().findById(id);
  }

  @Override
  public List<AccountConnector> findByUserId(String userId) {
    return getAccountConnectorDependentRepository().findByUserId(userId);
  }

  @Override
  public AccountConnector save(String userId, AccountConnector accountConnector) {
    return getAccountConnectorDependentRepository().save(userId, accountConnector);
  }

  @Override
  public List<AccountConnector> saveAll(String userId, List<AccountConnector> accountConnectors) {
    return getAccountConnectorDependentRepository().saveAll(userId, accountConnectors);
  }
}
