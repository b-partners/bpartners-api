package app.bpartners.api.repository.bridge.repository;

import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import java.util.List;

public interface BridgeTransactionRepository {
  List<BridgeTransaction> findAuthTransactions();

  BridgeTransaction findById(Long id);
}
