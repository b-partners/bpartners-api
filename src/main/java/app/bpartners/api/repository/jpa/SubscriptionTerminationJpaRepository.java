package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SubscriptionTermination;
import app.bpartners.api.model.subscription.SubscriptionTerminationStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionTerminationJpaRepository
    extends JpaRepository<SubscriptionTermination, String> {
  List<SubscriptionTermination> findByStatusIn(Collection<SubscriptionTerminationStatus> statuses);
}
