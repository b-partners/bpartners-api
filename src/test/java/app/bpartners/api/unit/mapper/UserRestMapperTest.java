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
import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.BillingInterval;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanDescription;
import app.bpartners.api.endpoint.rest.model.SubscriptionRenewalStatus;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.UserWhiteListed;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.model.Invoice;
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
  SubscriptionPlanRestMapper subscriptionPlanRestMapperMock = mock();
  TemporalUtils temporalUtils = new TemporalUtils();
  UserRestMapper subject =
      new UserRestMapper(
          accountRestMapperMock,
          subscriptionServiceMock,
          stripeInvoiceServiceMock,
          subscriptionEligibleJpaRepositoryMock,
          userWhiteListedJpaRepositoryMock,
          temporalUtils,
          subscriptionPlanRestMapperMock);

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
        UserSubscriptionEligible.builder().trialPeriodDays(7).eligibleFrom(LocalDate.now()).build();
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
  void user_v2_subscription_mapped_with_actual_plan() {
    var now = now();
    var actualProduct = SubscriptionProduct.builder().id("plan_id").name("Premium").build();
    var planDescription = new SubscriptionPlanDescription().id("plan_id").name("Premium");
    when(subscriptionPlanRestMapperMock.toRestDescription(actualProduct))
        .thenReturn(planDescription);
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(UserSubscription.builder().subscriptions(List.of()).build());

    var domain =
        User.builder()
            .roles(List.of())
            .paymentMethodExists(true)
            .subscriptionProducts(
                List.of(
                    UserSubscriptionProduct.builder()
                        .id(randomUUID().toString())
                        .subscriptionProduct(actualProduct)
                        .creationDatetime(now)
                        .subscriptionEndDatetime(null)
                        .build()))
            .build();

    var actual = subject.toRestV2(domain);

    assertEquals(planDescription, actual.getSubscription().getPlan());
  }

  @Test
  void user_v2_subscription_reports_the_persisted_billing_interval() {
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(UserSubscription.builder().subscriptions(List.of()).build());
    var domain =
        User.builder()
            .roles(List.of())
            .paymentMethodExists(true)
            .subscriptionProducts(
                List.of(
                    UserSubscriptionProduct.builder()
                        .id(randomUUID().toString())
                        .subscriptionProduct(SubscriptionProduct.builder().id("plan_id").build())
                        .billingInterval(
                            app.bpartners.api.model.subscription.BillingInterval.YEARLY)
                        .creationDatetime(now())
                        .build()))
            .build();

    var actual = subject.toRestV2(domain);

    assertEquals(BillingInterval.YEARLY, actual.getSubscription().getBillingInterval());
  }

  @Test
  void user_v2_subscription_falls_back_on_the_stripe_billing_interval() {
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .e2Id("stripe_subscription_id")
                            .startDatetime(now())
                            .billingInterval(
                                app.bpartners.api.model.subscription.BillingInterval.YEARLY)
                            .build()))
                .build());
    var domain = User.builder().roles(List.of()).paymentMethodExists(true).build();

    var actual = subject.toRestV2(domain);

    assertEquals(BillingInterval.YEARLY, actual.getSubscription().getBillingInterval());
  }

  @Test
  void user_v2_subscription_billing_interval_null_while_nothing_has_been_paid() {
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(List.of(Subscription.builder().startDatetime(now()).build()))
                .build());
    var domain = User.builder().roles(List.of()).paymentMethodExists(true).build();

    var actual = subject.toRestV2(domain);

    assertNull(actual.getSubscription().getBillingInterval());
  }

  @Test
  void user_v2_subscription_plan_null_when_no_actual_subscription_product() {
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(UserSubscription.builder().subscriptions(List.of()).build());

    var domain = User.builder().roles(List.of()).paymentMethodExists(true).build();

    var actual = subject.toRestV2(domain);

    assertNull(actual.getSubscription().getPlan());
    verify(subscriptionPlanRestMapperMock, never()).toRestDescription(any());
  }

  @Test
  void user_subscription_mapped_with_actual_plan() {
    var now = now();
    var actualProduct = SubscriptionProduct.builder().id("plan_id").name("Premium").build();
    var planDescription = new SubscriptionPlanDescription().id("plan_id").name("Premium");
    when(subscriptionPlanRestMapperMock.toRestDescription(actualProduct))
        .thenReturn(planDescription);
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(UserSubscription.builder().subscriptions(List.of()).build());

    var domain =
        User.builder()
            .roles(List.of())
            .paymentMethodExists(true)
            .subscriptionProducts(
                List.of(
                    UserSubscriptionProduct.builder()
                        .id(randomUUID().toString())
                        .subscriptionProduct(actualProduct)
                        .creationDatetime(now)
                        .subscriptionEndDatetime(null)
                        .build()))
            .build();

    var actual = subject.toRest(domain);

    assertEquals(planDescription, actual.getSubscription().getPlan());
  }

  @Test
  void user_subscription_plan_null_when_no_actual_subscription_product() {
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(UserSubscription.builder().subscriptions(List.of()).build());

    var domain = User.builder().roles(List.of()).paymentMethodExists(true).build();

    var actual = subject.toRest(domain);

    assertNull(actual.getSubscription().getPlan());
    verify(subscriptionPlanRestMapperMock, never()).toRestDescription(any());
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
  void user_subscription_mapped_as_cancelled_when_latest_subscription_is_cancelled() {
    Instant now = now();
    Instant expectedEndDatetime = now.plus(30L, DAYS);
    // A subscription left but still served until period end (real cancel-at-period-end, or a
    // schedule flagged for cancellation after its first invoice) is still active until endDatetime.
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .status(Subscription.SubscriptionStatus.CANCELED)
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
    assertEquals(CANCELLED, actual.getSubscriptionStatus());
    assertEquals(CANCELLED, actualSubscription.getStatus());
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
  void
      user_subscription_mapped_with_active_when_eligible_but_white_listed_with_credit_analysis_scope() {
    var now = now();
    var userId = randomUUID().toString();
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    var userWhiteListedMock = mock(UserWhiteListed.class);

    reset(subscriptionEligibleJpaRepositoryMock, userWhiteListedJpaRepositoryMock);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
    when(userWhiteListedMock.getScopes()).thenReturn(List.of(CREDIT_ANALYSIS_NOT_REQUIRED));
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
  void unpaid_invoice_on_the_current_subscription_still_returns_unpaid() {
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
        .thenReturn(List.of(mock(Invoice.class)));

    var actual =
        subject.toRest(
            User.builder()
                .id(userId)
                .roles(List.of())
                .paymentMethodExists(true)
                .userSubscriptionId(stripeCustomerId)
                .build());

    assertEquals(UNPAID, actual.getSubscriptionStatus());
  }

  @Test
  void
      unpaid_invoice_check_is_scoped_to_the_latest_real_subscription_when_latest_is_a_synthetic_gap_subscription() {
    var now = now();
    var userId = randomUUID().toString();
    var stripeCustomerId = "cus_1";
    var oldRealSubscriptionE2Id = "sub_old";
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);

    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        // old, superseded subscription: still a real Stripe subscription
                        Subscription.builder()
                            .e2Id(oldRealSubscriptionE2Id)
                            .status(Subscription.SubscriptionStatus.UNPAID)
                            .active(false)
                            .startDatetime(now.minus(60L, DAYS))
                            .endDatetime(now.minus(30L, DAYS))
                            .build(),
                        // synthetic placeholder bridging until the scheduled subscription starts:
                        // no e2Id, but the most recent startDatetime
                        Subscription.builder()
                            .status(Subscription.SubscriptionStatus.ACTIVE)
                            .active(true)
                            .startDatetime(now)
                            .endDatetime(now.plus(30L, DAYS))
                            .build()))
                .build());
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(
            stripeCustomerId, oldRealSubscriptionE2Id))
        .thenReturn(List.of(mock(Invoice.class)));

    var actual =
        subject.toRest(
            User.builder()
                .id(userId)
                .roles(List.of())
                .paymentMethodExists(true)
                .userSubscriptionId(stripeCustomerId)
                .build());

    assertEquals(UNPAID, actual.getSubscriptionStatus());
    verify(stripeInvoiceServiceMock)
        .getUnpaidStripeInvoices(stripeCustomerId, oldRealSubscriptionE2Id);
    verify(stripeInvoiceServiceMock, never()).getUnpaidStripeInvoices(stripeCustomerId, null);
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

  private User givenUserServedBy(Subscription subscription, String stripeCustomerId) {
    var userId = randomUUID().toString();
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
    when(subscriptionServiceMock.getSubscriptionByUser(any()))
        .thenReturn(UserSubscription.builder().subscriptions(List.of(subscription)).build());
    return User.builder()
        .id(userId)
        .roles(List.of())
        .paymentMethodExists(true)
        .userSubscriptionId(stripeCustomerId)
        .build();
  }

  @Test
  void renewal_status_is_will_renew_on_a_running_subscription() {
    var now = now();
    var domain =
        givenUserServedBy(
            Subscription.builder()
                .e2Id("sub_current")
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .active(true)
                .startDatetime(now)
                .endDatetime(now.plus(30L, DAYS))
                .build(),
            "cus_1");

    var actual = subject.toRest(domain).getSubscription();

    assertEquals(SubscriptionRenewalStatus.WILL_RENEW, actual.getRenewalStatus());
    assertNull(actual.getCancellationDatetime());
  }

  @Test
  void renewal_status_is_cancelled_at_period_end_while_the_subscription_is_still_served() {
    var now = now();
    var cancellationDatetime = now.minus(2L, DAYS);
    var domain =
        givenUserServedBy(
            Subscription.builder()
                .e2Id("sub_current")
                .status(Subscription.SubscriptionStatus.CANCELED)
                .active(true)
                .startDatetime(now.minus(10L, DAYS))
                .endDatetime(now.plus(20L, DAYS))
                .cancellationDatetime(cancellationDatetime)
                .build(),
            "cus_1");

    var actual = subject.toRest(domain).getSubscription();

    assertEquals(SubscriptionRenewalStatus.CANCELLED_AT_PERIOD_END, actual.getRenewalStatus());
    assertEquals(cancellationDatetime, actual.getCancellationDatetime());
    assertEquals(CANCELLED, actual.getStatus());
  }

  @Test
  void renewal_status_is_terminated_once_the_period_ended() {
    var now = now();
    var domain =
        givenUserServedBy(
            Subscription.builder()
                .e2Id("sub_current")
                .status(Subscription.SubscriptionStatus.CANCELED)
                .active(true)
                .startDatetime(now.minus(40L, DAYS))
                .endDatetime(now.minus(10L, DAYS))
                .cancellationDatetime(now.minus(35L, DAYS))
                .build(),
            "cus_1");

    var actual = subject.toRest(domain).getSubscription();

    assertEquals(SubscriptionRenewalStatus.TERMINATED, actual.getRenewalStatus());
  }

  @Test
  void next_subscription_is_reported_from_the_pending_schedule() {
    var now = now();
    var nextPeriodStart = now.plus(20L, DAYS);
    var scheduledPlan = SubscriptionProduct.builder().id("pro_plan_id").build();
    var scheduledPlanDescription = new SubscriptionPlanDescription().id("pro_plan_id");
    when(subscriptionPlanRestMapperMock.toRestDescription(scheduledPlan))
        .thenReturn(scheduledPlanDescription);
    var domain =
        givenUserServedBy(
            Subscription.builder()
                .e2Id("sub_current")
                .status(Subscription.SubscriptionStatus.CANCELED)
                .active(true)
                .startDatetime(now.minus(10L, DAYS))
                .endDatetime(nextPeriodStart)
                .build(),
            "cus_1");
    when(subscriptionServiceMock.getScheduledSubscription("cus_1"))
        .thenReturn(
            Optional.of(
                Subscription.builder()
                    .e2Id("sub_sched_1")
                    .subscriptionProduct(scheduledPlan)
                    .billingInterval(app.bpartners.api.model.subscription.BillingInterval.YEARLY)
                    .startDatetime(nextPeriodStart)
                    .build()));

    var actual = subject.toRest(domain);

    assertEquals(
        SubscriptionRenewalStatus.CANCELLED_AT_PERIOD_END,
        actual.getSubscription().getRenewalStatus());
    var next = actual.getNextSubscription();
    assertNotNull(next);
    assertEquals(scheduledPlanDescription, next.getPlan());
    assertEquals(BillingInterval.YEARLY, next.getBillingInterval());
    assertEquals(nextPeriodStart, next.getStart());
  }

  @Test
  void next_subscription_is_not_looked_up_on_a_subscription_that_renews() {
    var now = now();
    var domain =
        givenUserServedBy(
            Subscription.builder()
                .e2Id("sub_current")
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .active(true)
                .startDatetime(now)
                .endDatetime(now.plus(20L, DAYS))
                .build(),
            "cus_1");

    var actual = subject.toRest(domain);

    assertEquals(SubscriptionRenewalStatus.WILL_RENEW, actual.getSubscription().getRenewalStatus());
    assertNull(actual.getNextSubscription());
    verify(subscriptionServiceMock, never()).getScheduledSubscription(any());
  }

  @Test
  void next_subscription_is_null_when_the_schedule_is_already_the_served_subscription() {
    var now = now();
    var scheduleStart = now.minus(1L, DAYS);
    var userId = randomUUID().toString();
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
                            .e2Id("sub_cancelled")
                            .status(Subscription.SubscriptionStatus.CANCELED)
                            .active(true)
                            .startDatetime(now.minus(30L, DAYS))
                            .endDatetime(scheduleStart)
                            .build(),
                        // synthetic subscription mirroring the schedule, it carries no Stripe id
                        Subscription.builder()
                            .status(Subscription.SubscriptionStatus.ACTIVE)
                            .active(true)
                            .startDatetime(scheduleStart)
                            .endDatetime(now.plus(20L, DAYS))
                            .build()))
                .build());
    var domain =
        User.builder()
            .id(userId)
            .roles(List.of())
            .paymentMethodExists(true)
            .userSubscriptionId("cus_1")
            .build();

    var actual = subject.toRest(domain);

    assertEquals(
        SubscriptionRenewalStatus.CANCELLED_AT_PERIOD_END,
        actual.getSubscription().getRenewalStatus());
    assertNull(actual.getNextSubscription());
    verify(subscriptionServiceMock, never()).getScheduledSubscription(any());
  }
}
