package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.UserTokenRepository;
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
  private final UserTokenRepository userTokenRepository;

  @Override
  public List<AccountConnector> findByBearer(String bearer) {
    return findByBearerWithUser(bearer, AuthProvider.getAuthenticatedUser());
  }

  @Override
  public List<AccountConnector> findByUserId(String userId) {
    String bearer = AuthProvider.getBearer();
    User authenticated = AuthProvider.getAuthenticatedUser();
    if (bearer == null && authenticated == null) {
      UserToken userToken = userTokenRepository.getLatestTokenByUserId(userId);
      if (userToken != null) {
        bearer = userToken.getAccessToken();
        authenticated = userToken.getUser();
      }
    }
    return bearer == null
        || getAuthenticatedUserId() == null
        || !getAuthenticatedUserId().equals(userId)
        ? List.of()
        : findByBearerWithUser(bearer, authenticated);
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

  private List<AccountConnector> findByBearerWithUser(String bearer, User authenticated) {
    List<AccountConnector> connectors = bridgeApi.findAccountsByToken(bearer).stream()
        .map(accountMapper::toConnector)
        .collect(Collectors.toList());
    if (!connectors.isEmpty()) {
      if (authenticated.getBankConnectionId() == null) {
        bankRepository.updateBankConnection(authenticated);
      }
    }
    return connectors;
  }

}
