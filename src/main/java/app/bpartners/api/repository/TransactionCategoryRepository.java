package app.bpartners.api.repository;

import app.bpartners.api.model.TransactionCategory;
import java.time.LocalDate;
import java.util.List;

public interface TransactionCategoryRepository {

  List<TransactionCategory> findAllByIdAccount(
      String idAccount, LocalDate startDate, LocalDate endDate);

  List<TransactionCategory> saveAll(List<TransactionCategory> toCreate);

  TransactionCategory findByIdTransaction(String idTransaction);

}
