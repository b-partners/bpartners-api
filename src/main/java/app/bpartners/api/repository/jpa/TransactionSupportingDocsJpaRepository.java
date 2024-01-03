package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransactionSupportingDocs;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionSupportingDocsJpaRepository
    extends JpaRepository<HTransactionSupportingDocs, String> {
  List<HTransactionSupportingDocs> findAllByIdTransaction(String idTransaction);
}
