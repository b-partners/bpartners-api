package app.bpartners.api.repository.jpa;


import app.bpartners.api.repository.jpa.model.HUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<HUser, String> {
  List<HUser> findByAccessToken(String token);

  Optional<HUser> findByEmail(String email);

  Optional<HUser> findUserBySwanUserId(String swanUserId);
}
