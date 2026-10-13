package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionUpgradeJpaRepository
    extends JpaRepository<UserSubscriptionUpgrade, String> {
  List<UserSubscriptionUpgrade> findAllByUserIdOrderByCreationDatetimeDesc(String userId);

  Optional<UserSubscriptionUpgrade> findByStripeProrationInvoiceId(String stripeProrationInvoiceId);
}
