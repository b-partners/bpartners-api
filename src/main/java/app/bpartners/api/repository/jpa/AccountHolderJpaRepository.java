package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAccountHolder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountHolderJpaRepository extends JpaRepository<HAccountHolder, String> {
  List<HAccountHolder> findAllByAccountId(String accountId);

  Optional<HAccountHolder> findByAccountId(String accountId);
}
