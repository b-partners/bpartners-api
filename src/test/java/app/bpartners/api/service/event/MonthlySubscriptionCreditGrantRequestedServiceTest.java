package app.bpartners.api.service.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantRequested;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MonthlySubscriptionCreditGrantRequestedServiceTest {
  UserSubscriptionProductService userSubscriptionProductService = mock();
  CreditGrantService creditGrantService = mock();
  MonthlySubscriptionCreditGrantRequestedService subject =
      new MonthlySubscriptionCreditGrantRequestedService(
          userSubscriptionProductService, creditGrantService);

  @Test
  void grants_the_credits_included_in_the_plan_active_at_renewal() {
    var activePlan = SubscriptionProduct.builder().id("plan_id").build();
    when(userSubscriptionProductService.findActiveSubscriptionProduct("user_id"))
        .thenReturn(Optional.of(activePlan));

    subject.accept(MonthlySubscriptionCreditGrantRequested.builder().userId("user_id").build());

    verify(creditGrantService).grantIncludedCredits("user_id", activePlan);
  }

  @Test
  void grants_nothing_when_the_subscription_ended_before_the_renewal() {
    when(userSubscriptionProductService.findActiveSubscriptionProduct("user_id"))
        .thenReturn(Optional.empty());

    subject.accept(MonthlySubscriptionCreditGrantRequested.builder().userId("user_id").build());

    verify(creditGrantService, never()).grantIncludedCredits(any(), any());
  }
}
