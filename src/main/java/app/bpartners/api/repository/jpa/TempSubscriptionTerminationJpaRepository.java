package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SubscriptionTerminationStatus;
import app.bpartners.api.model.subscription.TempSubscriptionTermination;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TempSubscriptionTerminationJpaRepository
    extends JpaRepository<TempSubscriptionTermination, String> {
  List<TempSubscriptionTermination> findByStatusIn(
      Collection<SubscriptionTerminationStatus> statuses);
}
