package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import static javax.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface AccountJpaRepository extends JpaRepository<HAccount, String> {
  List<HAccount> findByUser_Id(String userId);

  @Lock(PESSIMISTIC_WRITE)
  List<HAccount> findAllByExternalId(String externalId);

  @Lock(PESSIMISTIC_WRITE)
  List<HAccount> findAllByUser_IdAndIban(String idUser, String iban);

  @Lock(PESSIMISTIC_WRITE)
  List<HAccount> findAllByUser_IdAndNameContainingIgnoreCaseAndIdBank(String idUser,
                                                                      String name,
                                                                      String idBank);
}
