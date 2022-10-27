package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAccountHolder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountHolderJpaRepository extends JpaRepository<HAccountHolder, String> {
  List<HAccountHolder> findByAccountId(String accountId);
}
