package app.bpartners.api.repository;

import app.bpartners.api.model.Transaction;
import java.util.List;

public interface TransactionRepository {
  List<Transaction> findByAccountId(String id); //TODO: Change to accounts/id/transactions
}
