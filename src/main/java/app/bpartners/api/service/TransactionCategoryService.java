package app.bpartners.api.service;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.TransactionCategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionCategoryService {
  private final TransactionCategoryRepository repository;

  public TransactionCategory getTransactionCategoryByIdTransaction(String idTransaction) {
    return repository.findByIdTransaction(idTransaction);
  }

  public TransactionCategory saveTransactionCategory(
      String transactionId,
      TransactionCategory toCreate) {
    return repository.save(transactionId, toCreate);
  }
}
