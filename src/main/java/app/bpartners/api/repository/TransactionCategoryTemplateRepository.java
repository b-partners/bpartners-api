package app.bpartners.api.repository;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.TransactionCategoryTemplate;

public interface TransactionCategoryTemplateRepository {
  TransactionCategoryTemplate findByTypeAndVat(String type, Fraction vat);
}
