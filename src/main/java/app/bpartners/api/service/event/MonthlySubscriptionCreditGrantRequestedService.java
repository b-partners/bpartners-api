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
    var activeProduct = userSubscriptionProductService.findActiveUserSubscriptionProduct(userId);
    if (activeProduct.isEmpty()) {
      log.info("User(id={}) has no active subscription plan anymore, skipping grant", userId);
      return;
    }
    if (activeProduct.get().isTrial()) {
      log.info(
          "User(id={}) is on a free trial, no recurring subscription credit is granted", userId);
      return;
    }
    creditGrantService.grantIncludedCredits(userId, activeProduct.get().getSubscriptionProduct());
  }
}
