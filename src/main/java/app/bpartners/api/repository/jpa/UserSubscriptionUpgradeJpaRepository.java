package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionUpgradeJpaRepository
    extends JpaRepository<UserSubscriptionUpgrade, String> {
  List<UserSubscriptionUpgrade> findAllByUserIdOrderByCreationDatetimeDesc(String userId);

  Optional<UserSubscriptionUpgrade> findByStripeProrationInvoiceId(String stripeProrationInvoiceId);

  List<UserSubscriptionUpgrade> findAllByPaymentExpirationDatetimeBefore(Instant expiration);

  List<UserSubscriptionUpgrade> findAllByQuoteExpirationDatetimeBefore(Instant expiration);

  @Query(
      value =
          "select 1 from (select pg_advisory_xact_lock(hashtext(:upgradeId), 1)) as lock_acquired",
      nativeQuery = true)
  Integer acquireLock(@Param("upgradeId") String upgradeId);
}
