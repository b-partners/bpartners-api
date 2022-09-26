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


  public List<TransactionCategory> getCategoriesByAccountAndUserDefined(
      String idAccount, boolean unique,
      boolean userDefined) {
    return repository.findByIdAccountAndUserDefined(idAccount, unique, userDefined);
  }

  public List<TransactionCategory> getCategoriesByAccount(String idAccount, boolean unique) {
    return repository.findByAccount(idAccount, unique);
  }

  public List<TransactionCategory> createCategories(
      List<TransactionCategory> toCreate) {
    return repository.saveAll(toCreate);
  }
}
