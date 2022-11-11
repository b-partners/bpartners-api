package app.bpartners.api.service;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionService {
  private final TransactionRepository repository;

  private final TransactionsSummaryRepository summaryRepository;

  public List<Transaction> getTransactionsByAccountId(String accountId) {
    return repository.findByAccountId(accountId);
  }

  public TransactionsSummary getTransactionsSummary(Integer year) {
    if (year == null) {
      year = LocalDate.now().getYear();
    }
    return summaryRepository.getByYear(year);
  }
}
