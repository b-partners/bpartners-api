package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransaction;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionJpaRepository extends JpaRepository<HTransaction, String> {
  List<HTransaction> findByIdAccountAndLabelContainingIgnoreCaseOrderByPaymentDateTimeDesc(
      String idAccount,
      String label,
      Pageable pageable);

  List<HTransaction> findByIdAccountOrderByPaymentDateTimeDesc(String idAccount, Pageable pageable);

  List<HTransaction> findAllByIdBridge(Long idBridge);
}
