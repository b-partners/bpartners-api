package app.bpartners.api.service.event;

import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.FAILED;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.PENDING;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.SUBSCRIPTION_ENDED;

import app.bpartners.api.endpoint.event.model.SubscriptionTerminationTriggered;
import app.bpartners.api.model.subscription.SubscriptionTermination;
import app.bpartners.api.repository.jpa.SubscriptionTerminationJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionTerminationService;
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
  private final SubscriptionTerminationJpaRepository repository;
  private final SubscriptionTerminationService subscriptionTerminationService;

  @Override
  public void accept(SubscriptionTerminationTriggered event) {
    var terminations = repository.findByStatusIn(List.of(PENDING, SUBSCRIPTION_ENDED, FAILED));
    log.info(
        "Subscription termination triggered, processing {} user(s) one after the other",
        terminations.size());
    terminations.stream()
        .map(SubscriptionTermination::getId)
        .forEach(subscriptionTerminationService::terminate);
  }
}
