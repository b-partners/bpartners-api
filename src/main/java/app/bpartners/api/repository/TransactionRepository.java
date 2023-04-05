package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.Transaction;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository {
  List<Transaction> findByAccountId(String id);

  Transaction findById(String transactionId);

  List<Transaction> findByAccountIdAndStatus(String id, TransactionStatus status);

  Transaction save(Transaction toSave);

  List<Transaction> findByAccountIdAndStatusBetweenInstants(
      String id, TransactionStatus status,
      Instant from, Instant to);

  Transaction getById(String idTransaction);
}
