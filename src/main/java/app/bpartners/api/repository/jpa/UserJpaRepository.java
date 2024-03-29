package app.bpartners.api.repository.jpa;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import app.bpartners.api.repository.jpa.model.HUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<HUser, String> {
  @Lock(PESSIMISTIC_WRITE)
  Optional<HUser> findByAccessToken(String token);

  Optional<HUser> findByEmail(String email);

  HUser getByEmail(String email);

  @Query("select u from HUser u join HAccount a" + " on u.id = a.user.id" + " where a.id = ?1")
  HUser getByAccountId(String accountId);

  @Lock(PESSIMISTIC_WRITE)
  @Query("select u from HUser u join HAccount a" + " on u.id = a.user.id" + " where a.id = ?1")
  HUser pwGetByAccountId(String accountId);

  @Lock(PESSIMISTIC_WRITE)
  HUser getHUserById(String id);
}
