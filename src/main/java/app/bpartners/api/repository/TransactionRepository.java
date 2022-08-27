package app.bpartners.api.repository;

import app.bpartners.api.model.Transaction;
import java.util.List;

public interface TransactionRepository {
  List<Transaction> findAll(); //TODO: will NEVER be used, remove
}
