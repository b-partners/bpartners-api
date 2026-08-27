package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.*;
import static app.bpartners.api.model.WhiteListScope.*;
import static java.time.Instant.now;
import static java.time.LocalTime.MAX;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.mapper.AccountRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserWhiteListed;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemporalUtils;
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
  StripeInvoiceService stripeInvoiceServiceMock = mock();
  UserWhiteListedJpaRepository userWhiteListedJpaRepositoryMock = mock();
  TemporalUtils temporalUtils = new TemporalUtils();
  UserRestMapper subject =
      new UserRestMapper(
          accountRestMapperMock,
          subscriptionServiceMock,
          stripeInvoiceServiceMock,
          subscriptionEligibleJpaRepositoryMock,
          userWhiteListedJpaRepositoryMock,
          temporalUtils);

  @BeforeEach
  void setUp() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any())).thenReturn(Optional.empty());
    when(userWhiteListedJpaRepositoryMock.findByUserId(any())).thenReturn(Optional.empty());
  }

  @Test
  void user_to_rest_check_subscription_start_end_and_status() {
    var domain = User.builder().status(ENABLED).roles(List.of()).paymentMethodExists(true).build();
    var userSubscriptionMock = mock(UserSubscription.class);
    var subscriptionEligible =
        UserSubscriptionEligible.builder().eligibleFrom(LocalDate.now()).build();
    when(userSubscriptionMock.getSubscriptions()).thenReturn(List.of());
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(UserSubscription.builder().build());
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.ofNullable(subscriptionEligible));
    when(subscriptionServiceMock.getSubscriptionByUser(domain)).thenReturn(userSubscriptionMock);

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
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));

    var actual = subject.toRest(User.builder().roles(List.of()).paymentMethodExists(true).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(ACTIVE, actualSubscription.getStatus());
    assertEquals(now, actualSubscription.getStart());
    assertEquals(expectedEndDatetime, actualSubscription.getEnd());
  }

  @Test
  void user_subscription_mapped_with_null_values() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.of(mock(UserSubscriptionEligible.class)));
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

    var actual = subject.toRest(User.builder().roles(List.of()).paymentMethodExists(true).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(EMPTY, actualSubscription.getStatus());
    assertNull(actualSubscription.getStart());
    assertNull(actualSubscription.getEnd());
  }

  @Test
  void user_subscription_mapped_with_default_values() {
    var now = now().plus(1L, DAYS);
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
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));

    var actual = subject.toRest(User.builder().roles(List.of()).paymentMethodExists(true).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(ACTIVE, actualSubscription.getStatus());
    assertNull(actualSubscription.getStart());
    assertNull(actualSubscription.getEnd());
  }

  @Test
  void user_subscription_mapped_with_payment_required_method_status() {
    var now = now();
    var userId = randomUUID().toString();
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);

    reset(subscriptionEligibleJpaRepositoryMock);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
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

    var actual =
        subject.toRest(
            User.builder().id(userId).roles(List.of()).paymentMethodExists(false).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(PAYMENT_METHOD_REQUIRED, actualSubscription.getStatus());
    assertNull(actualSubscription.getStart());
    assertNull(actualSubscription.getEnd());
  }

  @Test
  void
      user_subscription_mapped_with_payment_required_method_status_and_user_white_listed_not_containing_scope() {
    var now = now();
    var userId = randomUUID().toString();
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    var userWhiteListedMock = mock(UserWhiteListed.class);

    reset(subscriptionEligibleJpaRepositoryMock, userWhiteListedJpaRepositoryMock);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
    when(userWhiteListedMock.getScopes())
        .thenReturn(List.of(PROSPECT_EXISTING_MAIL_CREATION_ALLOWED));
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userWhiteListedMock));
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

    var actual =
        subject.toRest(
            User.builder().id(userId).roles(List.of()).paymentMethodExists(false).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(PAYMENT_METHOD_REQUIRED, actualSubscription.getStatus());
    assertNull(actualSubscription.getStart());
    assertNull(actualSubscription.getEnd());
  }

  @Test
  void
      user_subscription_mapped_with_active_when_eligible_but_white_listed_with_subscription_scope() {
    var now = now();
    var userId = randomUUID().toString();
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    var userWhiteListedMock = mock(UserWhiteListed.class);

    reset(subscriptionEligibleJpaRepositoryMock, userWhiteListedJpaRepositoryMock);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
    when(userWhiteListedMock.getScopes()).thenReturn(List.of(SUBSCRIPTION_VALIDATION_NOT_REQUIRED));
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userWhiteListedMock));
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

    var actual =
        subject.toRest(
            User.builder().id(userId).roles(List.of()).paymentMethodExists(false).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(ACTIVE, actualSubscription.getStatus());
    assertNotNull(actualSubscription.getStart());
    assertNotNull(actualSubscription.getEnd());
  }

  @Test
  void unpaid_invoice_check_is_scoped_to_the_current_subscription() {
    var now = now();
    var userId = randomUUID().toString();
    var stripeCustomerId = "cus_1";
    var currentSubscriptionE2Id = "sub_current";
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);

    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .e2Id(currentSubscriptionE2Id)
                            .status(Subscription.SubscriptionStatus.ACTIVE)
                            .active(true)
                            .startDatetime(now)
                            .endDatetime(now.plus(30L, DAYS))
                            .build()))
                .build());
    // no unpaid invoice on the CURRENT subscription, even though the customer may have an old,
    // unrelated unpaid/uncollectible invoice from a previous subscription period
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(
            stripeCustomerId, currentSubscriptionE2Id))
        .thenReturn(List.of());

    var actual =
        subject.toRest(
            User.builder()
                .id(userId)
                .roles(List.of())
                .paymentMethodExists(true)
                .userSubscriptionId(stripeCustomerId)
                .build());

    assertEquals(ACTIVE, actual.getSubscriptionStatus());
    verify(stripeInvoiceServiceMock)
        .getUnpaidStripeInvoices(stripeCustomerId, currentSubscriptionE2Id);
  }

  @Test
  void to_rest_v2_unpaid_invoice_check_is_scoped_to_the_current_subscription() {
    var now = now();
    var userId = randomUUID().toString();
    var stripeCustomerId = "cus_1";
    var currentSubscriptionE2Id = "sub_current";
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);

    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .e2Id(currentSubscriptionE2Id)
                            .status(Subscription.SubscriptionStatus.ACTIVE)
                            .active(true)
                            .startDatetime(now)
                            .endDatetime(now.plus(30L, DAYS))
                            .build()))
                .build());
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(
            stripeCustomerId, currentSubscriptionE2Id))
        .thenReturn(List.of());

    var actual =
        subject.toRestV2(
            User.builder()
                .id(userId)
                .roles(List.of())
                .paymentMethodExists(true)
                .userSubscriptionId(stripeCustomerId)
                .build());

    assertEquals(ACTIVE, actual.getSubscriptionStatus());
    verify(stripeInvoiceServiceMock)
        .getUnpaidStripeInvoices(stripeCustomerId, currentSubscriptionE2Id);
  }

  @Test
  void user_subscription_mapped_with_active_when_eligible_but_white_listed_with_api_key_scope() {
    var now = now();
    var userId = randomUUID().toString();
    var userWhiteListedMock = mock(UserWhiteListed.class);

    reset(subscriptionEligibleJpaRepositoryMock, userWhiteListedJpaRepositoryMock);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(mock(UserSubscriptionEligible.class)));
    when(userWhiteListedMock.getScopes()).thenReturn(List.of(API_KEY_NOT_RESTRICTED_BY_TRIAL));
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userWhiteListedMock));
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

    var actual =
        subject.toRest(
            User.builder().id(userId).roles(List.of()).paymentMethodExists(false).build());

    var actualSubscription = actual.getSubscription();
    assertEquals(ACTIVE, actualSubscription.getStatus());
    assertNull(actualSubscription.getStart());
    assertNull(actualSubscription.getEnd());
  }
}
