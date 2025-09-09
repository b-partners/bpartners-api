package app.bpartners.api.repository.connectors.account;

import static app.bpartners.api.endpoint.rest.security.AuthProvider.getAuthenticatedUserId;
import static app.bpartners.api.endpoint.rest.security.AuthProvider.userIsAuthenticated;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.model.AccountConnector;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

// TODO: add unit test
@Repository
@AllArgsConstructor
@Slf4j
public class BridgeAccountConnectorRepository implements AccountConnectorRepository {
  private final BridgeApi bridgeApi;
  private final AccountMapper accountMapper;
  private final SavableAccountConnectorRepository savableRepository;
  private final BankRepository bankRepository;
    private static final String UNSUPPORTED_ERROR_MESSAGE = "Unsupported: only saving methods are!";

    @Override
  public List<AccountConnector> findByBearer(String bearer) {
        throw new NotImplementedException(UNSUPPORTED_ERROR_MESSAGE);
    }

  @Override
  public List<AccountConnector> findByUserId(String userId) {
    if (!userIsAuthenticated()
        || getAuthenticatedUserId() == null
        || !getAuthenticatedUserId().equals(userId)) {
      return List.of();
    }
    return findByBearer(AuthProvider.getBearer());
  }

  @Override
  public AccountConnector save(String userId, AccountConnector accountConnector) {
    return savableRepository.save(userId, accountConnector);
  }

  @Override
  public List<AccountConnector> saveAll(String userId, List<AccountConnector> accountConnectors) {
    return savableRepository.saveAll(userId, accountConnectors);
  }

  @Override
  public AccountConnector findById(String id) {
      throw new NotImplementedException(UNSUPPORTED_ERROR_MESSAGE);
  }
}
