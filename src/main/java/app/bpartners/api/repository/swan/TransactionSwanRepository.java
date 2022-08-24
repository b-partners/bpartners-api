package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.schema.Transaction;
import java.util.List;

public interface TransactionSwanRepository {
  List<Transaction> getTransactions();
}
