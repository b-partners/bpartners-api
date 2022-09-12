package app.bpartners.api.service;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.TransactionRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionService {
  private final TransactionRepository repository;
  private final TransactionCategoryService categoryService;

  public List<Transaction> getTransactionsByAccountId(String accountId) {
    return repository.findByAccountId(accountId);
  }

  public Transaction getTransactionById(String id) {
    return repository.findById(id);
  }

  //TODO: Refers to the new API specification
  public Transaction modifyTransaction(
      String transactionId) {
    TransactionCategory transactionCategory =
        categoryService.getTransactionCategoryByIdTransaction(transactionId);
    categoryService.saveTransactionCategory(transactionId, transactionCategory);
    return getTransactionById(transactionId);
  }
}
