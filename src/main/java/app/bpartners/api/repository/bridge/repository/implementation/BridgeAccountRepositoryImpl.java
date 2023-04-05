package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
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
    //TODO: choose what account should be used
    if (accounts.size() > 1) {
      StringBuilder builder = getAccountMessageBuilder(accounts);
      log.warn(
          "[Bridge] Only one account is supported for now. "
              + "Therefore, these accounts were found :" + builder);
    }
    return accounts.isEmpty() ? List.of() : List.of(accounts.get(0));
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
}
