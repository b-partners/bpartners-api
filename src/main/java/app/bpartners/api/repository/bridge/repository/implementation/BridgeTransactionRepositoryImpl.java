package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.bridge.repository.BridgeTransactionRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BridgeTransactionRepositoryImpl implements BridgeTransactionRepository {
  private final BridgeApi bridgeApi;

  @Override
  public List<BridgeTransaction> findByBearer(String bearer) {
    return bearer == null ? List.of() : bridgeApi.findTransactionsUpdatedByToken(bearer);
  }

  @Override
  public BridgeTransaction findById(Long id) {
    return bridgeApi.findTransactionByIdAndToken(id, AuthProvider.getPrincipal().getBearer());
  }
}
