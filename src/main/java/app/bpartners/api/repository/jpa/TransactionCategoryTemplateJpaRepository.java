package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransactionCategoryTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryTemplateJpaRepository
    extends JpaRepository<HTransactionCategoryTemplate, String> {

  Optional<HTransactionCategoryTemplate> findByType(String type);
}
