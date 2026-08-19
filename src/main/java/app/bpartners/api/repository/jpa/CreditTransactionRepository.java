package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.credit.CreditTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, String> {
  List<CreditTransaction> findAllByUserId(String userId);
}
