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


  public List<TransactionCategory> getCategoriesByAccountAndUserDefined(
      String idAccount, boolean unique,
      boolean userDefined,
      String from,
      String to) {
    return repository.findByIdAccountAndUserDefined(idAccount, unique, userDefined,
        LocalDate.parse(from), LocalDate.parse(to));
  }

  public List<TransactionCategory> getCategoriesByAccount(String idAccount, boolean unique,
                                                          String from, String to) {
    return repository.findByAccount(idAccount, unique, LocalDate.parse(from), LocalDate.parse(to));
  }

  public List<TransactionCategory> createCategories(
      List<TransactionCategory> toCreate) {
    return repository.saveAll(toCreate);
  }
}
