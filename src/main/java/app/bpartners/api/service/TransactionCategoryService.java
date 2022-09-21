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
    //TODO : check if account is current user's account
    return repository.findByIdAccountAndUserDefined(idAccount, unique, userDefined);
  }

  public List<TransactionCategory> getCategoriesByAccount(String idAccount, boolean unique) {
    return repository.findByAccount(idAccount, unique);
  }

  public List<TransactionCategory> createCategories(
      String idAccount,
      List<TransactionCategory> toCreate) {
    //TODO : check if account is current user's account
    return repository.saveAll(toCreate);
  }
}
