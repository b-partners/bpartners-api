package app.bpartners.api.unit.model.subscription;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.UserSubscription;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserSubscriptionTest {
  @Test
  void get_latest_subscription_with_stripe_id_skips_synthetic_gap_subscription_without_e2_id() {
    var now = now();
    var oldRealSubscription =
        Subscription.builder()
            .e2Id("sub_old")
            .startDatetime(now.minus(60L, DAYS))
            .endDatetime(now.minus(30L, DAYS))
            .build();
    var syntheticGapSubscription =
        Subscription.builder().startDatetime(now).endDatetime(now.plus(30L, DAYS)).build();
    var subject =
        UserSubscription.builder()
            .subscriptions(List.of(oldRealSubscription, syntheticGapSubscription))
            .build();

    var actual = subject.getLatestSubscriptionWithStripeId();

    assertEquals(oldRealSubscription, actual);
    assertEquals(syntheticGapSubscription, subject.getLatestSubscription());
  }

  @Test
  void get_latest_subscription_with_stripe_id_returns_null_when_none_has_an_e2_id() {
    var subject =
        UserSubscription.builder()
            .subscriptions(List.of(Subscription.builder().startDatetime(now()).build()))
            .build();

    assertNull(subject.getLatestSubscriptionWithStripeId());
  }
}
