package app.bpartners.api.repository.jpa;


import app.bpartners.api.repository.jpa.model.HUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<HUser, String> {

  HUser getByPhoneNumber(String phoneNumber);

  Optional<HUser> findUserBySwanUserId(String swanUserId);
}
