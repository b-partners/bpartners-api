package app.bpartners.api.repository;

import app.bpartners.api.model.TransactionCategoryType;
import java.util.List;

public interface TransactionCategoryTypeRepository {
  List<TransactionCategoryType> findAll();

  List<TransactionCategoryType> saveAll(List<TransactionCategoryType> toCreate);

  TransactionCategoryType findById(String id);
}
