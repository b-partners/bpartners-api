package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.UserSubscriptionTrial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionTrialJpaRepository
    extends JpaRepository<UserSubscriptionTrial, String> {
  boolean existsByUserIdAndSubscriptionProductId(String userId, String subscriptionProductId);
}
