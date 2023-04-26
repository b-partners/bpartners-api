package app.bpartners.api.repository.jpa;


import app.bpartners.api.repository.jpa.model.HUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import static javax.persistence.LockModeType.PESSIMISTIC_WRITE;

@Repository
public interface UserJpaRepository extends JpaRepository<HUser, String> {
  @Lock(PESSIMISTIC_WRITE)
  List<HUser> findByAccessToken(String token);

  Optional<HUser> findByEmail(String email);

  HUser getByEmail(String email);

  Optional<HUser> findUserBySwanUserId(String swanUserId);
}
