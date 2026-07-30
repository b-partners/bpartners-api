package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionConsumptionLogJpaRepository
    extends JpaRepository<SubscriptionConsumptionLog, String> {
  List<SubscriptionConsumptionLog> findAllByUserIdAndCreationDatetimeBetween(
      String userId, Instant from, Instant to);

  List<SubscriptionConsumptionLog> findAllByUserIdAndConsumptionTypeAndCreationDatetimeBetween(
      String userId, SubscriptionConsumptionType consumptionType, Instant from, Instant to);
}
