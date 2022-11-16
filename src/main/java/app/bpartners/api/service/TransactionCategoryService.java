package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
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

  public List<TransactionCategory> getCategoriesByAccountAndType(
      String idAccount, LocalDate startDate, LocalDate endDate, TransactionTypeEnum type) {
    return repository.findAllByIdAccountAndType(idAccount, type, startDate, endDate);
  }

  public List<TransactionCategory> createCategories(
      List<TransactionCategory> toCreate) {
    return repository.saveAll(toCreate);
  }
}
