package app.bpartners.api.repository;

import app.bpartners.api.model.TransactionCategory;

public interface TransactionCategoryRepository {

  TransactionCategory findByIdTransaction(String idTransaction);

  TransactionCategory save(String transactionId, TransactionCategory toCreate);
}
