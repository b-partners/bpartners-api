package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransactionCategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryTypeJpaRepository
    extends JpaRepository<HTransactionCategoryType, String> {
}
