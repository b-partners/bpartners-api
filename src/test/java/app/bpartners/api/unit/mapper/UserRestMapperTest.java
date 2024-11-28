package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.*;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.AccountRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserRestMapperTest {
  AccountRestMapper accountRestMapperMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  UserRestMapper subject = new UserRestMapper(accountRestMapperMock, subscriptionServiceMock);

  @Test
  void user_subscription_mapped_with_subscription_values() {
    Instant now = now();
    Instant expectedEndDatetime = now.plus(30L, DAYS);
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .status(Subscription.SubscriptionStatus.ACTIVE)
                            .active(true)
                            .startDatetime(now)
                            .endDatetime(expectedEndDatetime)
                            .build()))
                .build());

    var actual = subject.toRest(User.builder().roles(List.of()).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(ACTIVE, actualSubscription.getStatus());
    assertEquals(now, actualSubscription.getStart());
    assertEquals(expectedEndDatetime, actualSubscription.getEnd());
  }

  @Test
  void user_subscription_mapped_with_trial() {
    var now = now();
    var expectedEndDatetime = now.plus(30L, DAYS);
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .status(Subscription.SubscriptionStatus.CANCELLED)
                            .active(true)
                            .freeTrialStart(now)
                            .freeTrialEnd(expectedEndDatetime)
                            .build()))
                .build());

    var actual = subject.toRest(User.builder().roles(List.of()).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(CANCELLED, actualSubscription.getStatus());
    assertEquals(now, actualSubscription.getStart());
    assertEquals(expectedEndDatetime, actualSubscription.getEnd());
  }

  @Test
  void user_subscription_mapped_with_null_values() {
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .status(Subscription.SubscriptionStatus.UNKNOWN)
                            .active(false)
                            .freeTrialStart(null)
                            .freeTrialEnd(null)
                            .startDatetime(null)
                            .endDatetime(null)
                            .build()))
                .build());

    var actual = subject.toRest(User.builder().roles(List.of()).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(EMPTY, actualSubscription.getStatus());
    assertNull(actualSubscription.getStart());
    assertNull(actualSubscription.getEnd());
  }

  @Test
  void user_subscription_mapped_with_default_values() {
    var now = now();
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .status(Subscription.SubscriptionStatus.ACTIVE)
                            .active(true)
                            .freeTrialStart(null)
                            .freeTrialEnd(null)
                            .startDatetime(now)
                            .endDatetime(now)
                            .build()))
                .build());

    var actual = subject.toRest(User.builder().roles(List.of()).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(ACTIVE, actualSubscription.getStatus());
    assertNull(actualSubscription.getStart());
    assertNull(actualSubscription.getEnd());
  }
}
