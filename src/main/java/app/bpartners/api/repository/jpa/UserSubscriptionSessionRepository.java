package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.UserSubscriptionSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionSessionRepository
    extends JpaRepository<UserSubscriptionSession, String> {
  List<UserSubscriptionSession> findAllByUserId(String userId);
}
