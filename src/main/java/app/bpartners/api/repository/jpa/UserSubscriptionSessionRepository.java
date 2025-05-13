package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.UserSubscriptionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionSessionRepository
    extends JpaRepository<UserSubscriptionSession, String> {
  UserSubscriptionSession findByUserId(String userId);
}
