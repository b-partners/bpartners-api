package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.UserStripeCustomerEmailCorrespondence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStripeCustomerEmailCorrespondenceJpaRepository
    extends JpaRepository<UserStripeCustomerEmailCorrespondence, String> {
  Optional<UserStripeCustomerEmailCorrespondence> findByUserId(String userId);
}
