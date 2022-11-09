package app.bpartners.api.repository;

import app.bpartners.api.model.TransactionCategoryTemplate;

public interface TransactionCategoryTemplateRepository {
  TransactionCategoryTemplate findByType(String type);
}
