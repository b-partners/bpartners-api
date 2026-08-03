package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillTriggered;
import app.bpartners.api.repository.UserRepository;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionProductBackfillTriggeredService
    implements Consumer<UserSubscriptionProductBackfillTriggered> {
  private final UserRepository userRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(UserSubscriptionProductBackfillTriggered event) {
    var userIds = userRepository.findEnabledUserIdsWithSubscription();
    log.info(
        "UserSubscriptionProduct backfill triggered, fanning out for {} user(s)", userIds.size());
    if (userIds.isEmpty()) {
      return;
    }
    eventProducer.accept(
        userIds.stream()
            .map(
                userId -> UserSubscriptionProductBackfillRequested.builder().userId(userId).build())
            .toList());
  }
}
