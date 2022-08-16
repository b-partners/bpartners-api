package app.bpartners.api.repository.jpa;


import app.bpartners.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<User, String> {
  User getUserBySwanUserId(String swanUserId);
}
