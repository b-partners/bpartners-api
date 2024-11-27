package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionEligibleJpaRepository
    extends JpaRepository<UserSubscriptionEligible, String> {
  Optional<UserSubscriptionEligible> findByUserId(String userId);
}
