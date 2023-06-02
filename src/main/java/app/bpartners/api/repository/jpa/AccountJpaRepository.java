package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import static javax.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface AccountJpaRepository extends JpaRepository<HAccount, String> {
  List<HAccount> findByUser_Id(String userId);

  @Lock(PESSIMISTIC_WRITE)
  Optional<HAccount> findByExternalId(String externalId);

  @Lock(PESSIMISTIC_WRITE)
  List<HAccount> findAllByExternalId(String externalId);
}
