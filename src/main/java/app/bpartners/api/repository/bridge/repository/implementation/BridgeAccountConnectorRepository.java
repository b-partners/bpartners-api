package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.implementation.SavableAccountConnectorRepository;
import app.bpartners.api.repository.model.AccountConnector;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.AuthProvider.getAuthenticatedUserId;
import static app.bpartners.api.endpoint.rest.security.AuthProvider.userIsAuthenticated;

//TODO: add unit test
@Repository
@AllArgsConstructor
@Slf4j
public class BridgeAccountConnectorRepository implements AccountConnectorRepository {
  private final BridgeApi bridgeApi;
  private final AccountMapper accountMapper;
  private final SavableAccountConnectorRepository savableRepository;
  private final BankRepository bankRepository;

  @Override
  public List<AccountConnector> findByBearer(String bearer) {
    List<AccountConnector> connectors = bridgeApi.findAccountsByToken(bearer).stream()
        .map(accountMapper::toConnector)
        .collect(Collectors.toList());
    if (!connectors.isEmpty()) {
      User authenticated = AuthProvider.getAuthenticatedUser();
      if (authenticated.getBankConnectionId() == null) {
        bankRepository.updateBankConnection(authenticated);
      }
    }
    return connectors;
  }

  @Override
  public List<AccountConnector> findByUserId(String userId) {
    if (!userIsAuthenticated() || getAuthenticatedUserId() == null
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
    try {
      Long bridgeId = Long.valueOf(id);
      return !userIsAuthenticated() ? null
          : accountMapper.toConnector(
          bridgeApi.findByAccountById(bridgeId, AuthProvider.getBearer()));
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
