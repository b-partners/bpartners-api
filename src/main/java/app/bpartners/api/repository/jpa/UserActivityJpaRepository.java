package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.activity.UserActivity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityJpaRepository extends JpaRepository<UserActivity, String> {
  List<UserActivity> findAllByUserIdAndCreationDatetimeBetween(
      String userId, Instant from, Instant to);
}
