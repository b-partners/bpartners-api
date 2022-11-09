package app.bpartners.api.service;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.TransactionCategoryRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionCategoryService {
  private final TransactionCategoryRepository repository;

  public List<TransactionCategory> getCategoriesByAccount(
      String idAccount, LocalDate startDate, LocalDate endDate) {
    return repository.findAllByIdAccount(idAccount, startDate, endDate);
  }

  public List<TransactionCategory> createCategories(
      List<TransactionCategory> toCreate) {
    return repository.saveAll(toCreate);
  }
}
