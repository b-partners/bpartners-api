package app.bpartners.api.repository.jpa;


import app.bpartners.api.repository.jpa.model.HUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<HUser, String> {
  HUser getByPhoneNumber(String phoneNumber);

  @Query(value = "select u.* from \"user\" u join ("
      + "select id_user from user_token where access_token = ?1) as ut "
      + "on ut.id_user = u.id ", nativeQuery = true)
  HUser findByToken(String token);

  Optional<HUser> findByEmail(String email);

  Optional<HUser> findUserBySwanUserId(String swanUserId);
}
