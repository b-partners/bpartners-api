package app.bpartners.api.repository.jpa;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import app.bpartners.api.repository.jpa.model.HAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AccountJpaRepository extends JpaRepository<HAccount, String> {
  List<HAccount> findByUser_Id(String userId);

  @Lock(PESSIMISTIC_WRITE)
  List<HAccount> findAllByExternalId(String externalId);

  @Lock(PESSIMISTIC_WRITE)
  List<HAccount> findAllByIban(String iban);

  @Lock(PESSIMISTIC_WRITE)
  List<HAccount> findAllByNameContainingIgnoreCaseAndIdBank(String name, String idBank);

  @Lock(PESSIMISTIC_WRITE)
  List<HAccount> findAllByNameContainingIgnoreCase(String name);
}
