package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantRequested;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlySubscriptionCreditGrantRequestedService
    implements Consumer<MonthlySubscriptionCreditGrantRequested> {
  private final UserSubscriptionProductService userSubscriptionProductService;
  private final CreditGrantService creditGrantService;

  @Override
  public void accept(MonthlySubscriptionCreditGrantRequested event) {
    var userId = event.getUserId();
    var activePlan = userSubscriptionProductService.findActiveSubscriptionProduct(userId);
    if (activePlan.isEmpty()) {
      log.info("User(id={}) has no active subscription plan anymore, skipping grant", userId);
      return;
    }
    creditGrantService.grantIncludedCredits(userId, activePlan.get());
  }
}
