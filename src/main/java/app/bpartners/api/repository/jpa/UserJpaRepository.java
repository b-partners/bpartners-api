package app.bpartners.api.repository.jpa;


import app.bpartners.api.model.entity.HUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<HUser, String> {
  HUser getUserBySwanUserId(String swanUserId);
}
