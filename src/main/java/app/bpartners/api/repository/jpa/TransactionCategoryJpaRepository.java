package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.entity.HTransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryJpaRepository
    extends JpaRepository<HTransactionCategory, String> {
}
