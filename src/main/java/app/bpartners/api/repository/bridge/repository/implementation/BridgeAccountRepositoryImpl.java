package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.repository.BridgeAccountRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class BridgeAccountRepositoryImpl implements BridgeAccountRepository {
  private final BridgeApi bridgeApi;

  @Override
  public List<BridgeAccount> findByBearer(String bearer) {
    List<BridgeAccount> accounts = bridgeApi.findAccountsByToken(bearer);
    if (accounts.isEmpty()) {
      return List.of();
    }
    User authenticatedUser = AuthProvider.getPrincipal().getUser();
    if (authenticatedUser.getPreferredAccountId() != null) {
      return List.of(accounts.stream()
          .filter(bridgeAccount ->
              String.valueOf(bridgeAccount.getId())
                  .equals(authenticatedUser.getPreferredAccountId()))
          .findAny()
          .orElseGet(() -> {
            log.warn("User(id=" + authenticatedUser.getId() + ", preferred_account_external_id="
                + authenticatedUser.getPreferredAccountId() + ") has bad account external ID."
                + getDefaultAccountMessage(accounts, authenticatedUser));
            return accounts.get(0);
          }));
    }
    if (accounts.size() > 1) {
      StringBuilder builder = getAccountMessageBuilder(accounts);
      log.warn(
          "[Bridge] Only one account is supported for now. "
              + "Therefore, these accounts were found :" + builder);
    }
    log.warn(getDefaultAccountMessage(accounts, authenticatedUser));
    return List.of(accounts.get(0));
  }

  @Override
  public List<BridgeAccount> findAllByAuthenticatedUser() {
    return findByBearer(AuthProvider.getPrincipal().getBearer());
  }

  @Override
  public BridgeAccount findById(Long id) {
    return bridgeApi.findByAccountById(id, AuthProvider.getPrincipal().getBearer());
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
