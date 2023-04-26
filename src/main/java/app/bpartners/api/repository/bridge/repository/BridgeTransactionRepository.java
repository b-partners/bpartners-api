package app.bpartners.api.repository.bridge.repository;

import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import java.util.List;

public interface BridgeTransactionRepository {

  List<BridgeTransaction> findByBearer(String bearer);

  BridgeTransaction findById(Long id);
}
