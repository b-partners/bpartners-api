package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionConsumptionLogJpaRepository
    extends JpaRepository<SubscriptionConsumptionLog, String> {
  List<SubscriptionConsumptionLog> findAllByUserIdAndCreationDatetimeBetween(
      String userId, Instant from, Instant to);
}
