package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.model.Transaction;
import java.util.List;

public interface TransactionSwanRepository {
  List<Transaction> getTransactions();

  Transaction findById(String id);
}
