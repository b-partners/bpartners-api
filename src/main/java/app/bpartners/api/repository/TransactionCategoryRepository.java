package app.bpartners.api.repository;

import app.bpartners.api.model.TransactionCategory;
import java.util.List;

public interface TransactionCategoryRepository {
  List<TransactionCategory> findAll();

  List<TransactionCategory> saveAll(List<TransactionCategory> toCreate);
}
