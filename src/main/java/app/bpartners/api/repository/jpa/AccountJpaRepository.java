package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<HAccount, String> {
  Optional<HAccount> findByExternalId(String externalId);

  List<HAccount> findByUser_Id(String userId);
}
