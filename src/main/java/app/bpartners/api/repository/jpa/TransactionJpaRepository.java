package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionJpaRepository extends JpaRepository<HTransaction, String> {
  Optional<HTransaction> findByIdSwan(String idSwan);

  List<HTransaction> findAllByIdAccount(String idAccount);

}
