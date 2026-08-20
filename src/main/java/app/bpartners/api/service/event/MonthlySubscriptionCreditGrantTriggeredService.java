package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantRequested;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantTriggered;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlySubscriptionCreditGrantTriggeredService
    implements Consumer<MonthlySubscriptionCreditGrantTriggered> {
  private final UserSubscriptionProductService userSubscriptionProductService;
  private final EventProducer eventProducer;

  @Override
  public void accept(MonthlySubscriptionCreditGrantTriggered event) {
    var userIds = userSubscriptionProductService.findUserIdsWithActiveSubscriptionProduct();
    log.info(
        "Monthly subscription credit grant triggered, fanning out for {} subscribed user(s)",
        userIds.size());
    if (userIds.isEmpty()) {
      return;
    }
    eventProducer.accept(
        userIds.stream()
            .map(userId -> MonthlySubscriptionCreditGrantRequested.builder().userId(userId).build())
            .toList());
  }
}
