package app.bpartners.api.repository;

import app.bpartners.api.model.TransactionCategory;
import java.time.LocalDate;
import java.util.List;

public interface TransactionCategoryRepository {

  List<TransactionCategory> findByIdAccountAndUserDefined(String idAccount, boolean unique,
                                                          boolean userDefined, LocalDate from,
                                                          LocalDate to);

  List<TransactionCategory> findByAccount(String idAccount, boolean unique, LocalDate from,
                                          LocalDate to);

  List<TransactionCategory> saveAll(List<TransactionCategory> toCreate);

  TransactionCategory findByIdTransaction(String idTransaction);
}
