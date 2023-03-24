package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.repository.BridgeAccountRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BridgeAccountRepositoryImpl implements BridgeAccountRepository {
  private final BridgeApi bridgeApi;

  @Override
  public List<BridgeAccount> findByBearer(String bearer) {
    List<BridgeAccount> accounts = bridgeApi.findAccountsByToken(bearer);
    return accounts.isEmpty() ? List.of() : List.of(accounts.get(0));
  }

  @Override
  public List<BridgeAccount> findAllByAuthenticatedUser() {
    return bridgeApi.findAccountsByToken(AuthProvider.getPrincipal().getBearer());
  }

  @Override
  public BridgeAccount findById(String id) {
    return bridgeApi.findByAccountById(id, AuthProvider.getPrincipal().getBearer());
  }
}
