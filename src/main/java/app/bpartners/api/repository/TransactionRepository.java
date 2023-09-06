package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.JustifyTransaction;
import app.bpartners.api.model.Transaction;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository {
  List<Transaction> findByIdAccount(String idAccount, String title, int page, int pageSize);

  List<Transaction> findPersistedByIdAccount(String idAccount, int page, int pageSize);

  List<Transaction> findByAccountId(String id);

  Transaction findById(String id);

  List<Transaction> findByAccountIdAndStatus(String id, TransactionStatus status);

  Transaction save(JustifyTransaction justifyTransaction);

  List<Transaction> findByAccountIdAndStatusBetweenInstants(
      String id, TransactionStatus status,
      Instant from, Instant to);

  Transaction getById(String idTransaction);

  void removeAll(List<Transaction> toRemove);

}
