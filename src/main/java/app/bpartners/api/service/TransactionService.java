package app.bpartners.api.service;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.repository.TransactionRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionService {
  private final TransactionRepository repository;

  public List<Transaction> getTransactions() {
    return repository.findByAccountId(null);
  }
}
