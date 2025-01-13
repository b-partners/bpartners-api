package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.*;
import static java.time.Instant.now;
import static java.time.LocalTime.MAX;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.AccountRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserRestMapperTest {
  AccountRestMapper accountRestMapperMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepositoryMock = mock();
  UserRestMapper subject =
      new UserRestMapper(
          accountRestMapperMock, subscriptionServiceMock, subscriptionEligibleJpaRepositoryMock);

  @BeforeEach
  void setUp() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any())).thenReturn(Optional.empty());
  }

  @Test
  void user_to_rest_check_subscription_start_end_and_status() {
    var domain = User.builder().status(ENABLED).roles(List.of()).build();
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(UserSubscription.builder().build());
    var subscriptionEligible =
        UserSubscriptionEligible.builder().eligibleFrom(LocalDate.now()).build();
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.ofNullable(subscriptionEligible));

    var actual = subject.toRest(domain);

    assertEquals(FREE_TRIAL, actual.getSubscriptionStatus());
    var parisZoneId = ZoneId.of("Europe/Paris");
    assertNotNull(subscriptionEligible);
    var subscriptionEnd =
        subscriptionEligible.getLatestTrialPeriodDate().atTime(MAX).atZone(parisZoneId).toInstant();
    assertEquals(subscriptionEnd, Objects.requireNonNull(actual.getSubscription()).getEnd());
    var subscriptionStart =
        subscriptionEligible.getEligibleFrom().atStartOfDay(parisZoneId).toInstant();
    assertEquals(subscriptionStart, actual.getSubscription().getStart());
  }

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
  void user_subscription_mapped_with_null_values() {
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .status(Subscription.SubscriptionStatus.UNKNOWN)
                            .active(false)
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
