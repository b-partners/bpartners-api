package app.bpartners.api.service;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.TransactionCategoryRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionCategoryService {
  private final TransactionCategoryRepository repository;

  public List<TransactionCategory> getTransactionCategories() {
    return repository.findAll();
  }

  public List<TransactionCategory> createTransactionCategories(List<TransactionCategory> toCreate) {
    return repository.saveAll(toCreate);
  }
}
