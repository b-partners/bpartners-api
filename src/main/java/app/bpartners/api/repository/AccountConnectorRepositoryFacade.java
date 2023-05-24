package app.bpartners.api.repository;

import app.bpartners.api.repository.bridge.repository.implementation.BridgeAccountConnectorRepository;
import app.bpartners.api.repository.model.AccountConnector;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@AllArgsConstructor
public class AccountConnectorRepositoryFacade implements AccountConnectorRepository {
  private final BridgeAccountConnectorRepository bridgeConnector;

  private AccountConnectorRepository getAccountConnectorDependentRepository() {
    return new CachedAccountConnectorRepository(bridgeConnector);
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
