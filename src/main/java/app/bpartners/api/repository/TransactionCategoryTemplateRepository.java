package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.TransactionCategoryTemplate;
import java.time.LocalDate;
import java.util.List;

public interface TransactionCategoryTemplateRepository {
  TransactionCategoryTemplate findByType(String type);

  List<TransactionCategoryTemplate> findAllByIdAccount(
      String idAccount, LocalDate begin, LocalDate end);

  List<TransactionCategoryTemplate> findAllByIdAccountAndType(
      String idAccount, TransactionTypeEnum typeEnum, LocalDate begin, LocalDate end);
}
