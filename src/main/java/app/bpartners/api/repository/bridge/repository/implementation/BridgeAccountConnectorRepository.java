package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.implementation.SavableAccountConnectorRepository;
import app.bpartners.api.repository.model.AccountConnector;
import java.util.List;
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

  @Override
  public List<AccountConnector> findByBearer(String bearer) {
    List<BridgeAccount> accounts = bridgeApi.findAccountsByToken(bearer);
    if (accounts.isEmpty()) {
      return List.of();
    }
    User authenticatedUser = AuthProvider.getPrincipal().getUser();
    if (authenticatedUser.getPreferredAccountId() != null) {
      return List.of(accountMapper.toConnector(accounts.stream()
          .filter(bridgeAccount ->
              String.valueOf(bridgeAccount.getId())
                  .equals(authenticatedUser.getPreferredAccountId()))
          .findAny()
          .orElseGet(() -> {
            log.warn("User(id=" + authenticatedUser.getId() + ", preferred_account_external_id="
                + authenticatedUser.getPreferredAccountId() + ") has bad account external ID."
                + getDefaultAccountMessage(accounts, authenticatedUser));
            return accounts.get(0);
          })));
    }
    if (accounts.size() > 1) {
      StringBuilder builder = getAccountMessageBuilder(accounts);
      log.warn(
          "[Bridge] Only one account is supported for now. "
              + "Therefore, these accounts were found :" + builder);
    }
    log.warn(getDefaultAccountMessage(accounts, authenticatedUser));
    AccountConnector accountConnector = accountMapper.toConnector(accounts.get(0));
    return List.of(accountConnector);
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
  public AccountConnector save(String idUser, AccountConnector accountConnector) {
    return savableRepository.save(idUser, accountConnector);
  }

  @Override
  public List<AccountConnector> saveAll(String idUser, List<AccountConnector> accountConnectors) {
    return savableRepository.saveAll(idUser, accountConnectors);
  }

  @Override
  public AccountConnector findById(String id) {
    try {
      Long bridgeId = Long.valueOf(id);
      return AuthProvider.getBearer() == null ? null
          : accountMapper.toConnector(
          bridgeApi.findByAccountById(bridgeId, AuthProvider.getBearer()));
      // /!\ case when provided ID is UUID, from Swan for example
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private StringBuilder getAccountMessageBuilder(List<BridgeAccount> accounts) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < accounts.size(); i++) {
      BridgeAccount bridgeAccount = accounts.get(i);
      builder.append("Account(id=")
          .append(bridgeAccount.getId())
          .append(",name=")
          .append(bridgeAccount.getName())
          .append(",status=")
          .append(bridgeAccount.getStatus())
          .append(")");
      if (i != accounts.size() - 1) {
        builder.append(" and ");
      }
    }
    return builder;
  }

  private static String getDefaultAccountMessage(List<BridgeAccount> accounts, User user) {
    return "Any preferred account found for user(id=" + user.getId() + ")."
        + " BridgeAccount(id=" + accounts.get(0).getId()
        + ",name=" + accounts.get(0).getName()
        + ", iban=" + accounts.get(0).getIban() + ") was chosen.";
  }
}
