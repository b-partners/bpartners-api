package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillTriggered;
import app.bpartners.api.service.subscription.UpcomingUserDebitService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionProductBackfillTriggeredService
    implements Consumer<UserSubscriptionProductBackfillTriggered> {
  private final UpcomingUserDebitService upcomingUserDebitService;
  private final EventProducer eventProducer;

  @Override
  public void accept(UserSubscriptionProductBackfillTriggered event) {
    log.info("UserSubscriptionProduct backfill triggered, computing billed users...");
    var billedUsers = upcomingUserDebitService.getUpcomingBilledUsers();
    log.info(
        "UserSubscriptionProduct backfill triggered for {} billed user(s)", billedUsers.size());
    billedUsers.forEach(
        user ->
            eventProducer.accept(
                List.of(
                    UserSubscriptionProductBackfillRequested.builder()
                        .userId(user.getId())
                        .build())));
  }
}
