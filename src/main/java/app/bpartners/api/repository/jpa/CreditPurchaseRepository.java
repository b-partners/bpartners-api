package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditPurchaseStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditPurchaseRepository extends JpaRepository<CreditPurchase, String> {
  List<CreditPurchase> findByUserIdAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
      String userId, Instant from, Instant to, Pageable pageable);

  List<CreditPurchase> findByStatusAndInvoiceIdIsNull(CreditPurchaseStatus status);

  List<CreditPurchase> findByUserIdAndStatusInAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
      String userId,
      List<CreditPurchaseStatus> statuses,
      Instant from,
      Instant to,
      Pageable pageable);
}
