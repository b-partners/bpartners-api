package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.model.SwanTransaction;
import java.util.List;

public interface TransactionSwanRepository {
  List<SwanTransaction> getTransactions();
}
