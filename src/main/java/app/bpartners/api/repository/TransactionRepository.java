package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.Transaction;
import java.util.List;

public interface TransactionRepository {
  List<Transaction> findByAccountId(String id);

  Transaction updateType(String swanTransactionId, TransactionTypeEnum type);

  Transaction findBySwanId(String swanTransactionId);
}
