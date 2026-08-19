package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, String> {
  List<CreditTransaction> findAllByUserId(String userId);

  List<CreditTransaction> findByUserIdAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
      String userId, Instant from, Instant to, Pageable pageable);

  List<CreditTransaction>
      findByUserIdAndTypeInAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
          String userId,
          List<CreditTransactionType> types,
          Instant from,
          Instant to,
          Pageable pageable);
}
