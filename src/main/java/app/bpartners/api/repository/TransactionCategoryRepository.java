package app.bpartners.api.repository;

import app.bpartners.api.model.TransactionCategory;
import java.util.List;

public interface TransactionCategoryRepository {

  List<TransactionCategory> findByIdAccountAndUserDefined(String idAccount, boolean unique,
                                                          boolean userDefined);

  List<TransactionCategory> findByAccount(String idAccount, boolean unique);

  List<TransactionCategory> saveAll(List<TransactionCategory> toCreate);

  TransactionCategory findByIdTransaction(String idTransaction);
}
