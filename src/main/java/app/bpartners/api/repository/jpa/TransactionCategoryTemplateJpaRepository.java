package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransactionCategoryTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryTemplateJpaRepository
    extends JpaRepository<HTransactionCategoryTemplate, String> {

  HTransactionCategoryTemplate findByTypeAndVat(String type, int vat);
}
