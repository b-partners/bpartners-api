package app.bpartners.api.service.event;

import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.FAILED;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.PENDING;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.SUBSCRIPTION_ENDED;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionTerminationRequested;
import app.bpartners.api.endpoint.event.model.SubscriptionTerminationTriggered;
import app.bpartners.api.model.subscription.TempSubscriptionTermination;
import app.bpartners.api.repository.jpa.TempSubscriptionTerminationJpaRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionTerminationTriggeredService
    implements Consumer<SubscriptionTerminationTriggered> {
  private final TempSubscriptionTerminationJpaRepository repository;
  private final EventProducer eventProducer;

  @Override
  public void accept(SubscriptionTerminationTriggered event) {
    var terminations = repository.findByStatusIn(List.of(PENDING, SUBSCRIPTION_ENDED, FAILED));
    log.info("Subscription termination triggered, fanning out for {} user(s)", terminations.size());
    if (terminations.isEmpty()) {
      return;
    }
    eventProducer.accept(
        terminations.stream()
            .map(TempSubscriptionTermination::getId)
            .map(id -> SubscriptionTerminationRequested.builder().terminationId(id).build())
            .toList());
  }
}
