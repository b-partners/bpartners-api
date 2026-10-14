package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionUpgradeInvoicingRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionUpgradeCompletionServiceTest {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final String USER_ID = "subscriber_id";
  private static final String STRIPE_CUSTOMER_ID = "cus_subscriber";
  private static final String UPGRADE_ID = "upgrade_id";
  private static final String INVOICE_ID = "in_proration";
  private static final String PAYMENT_METHOD_ID = "pm_default";
  private static final Instant UPGRADE_DATETIME =
      LocalDate.of(2026, 10, 12).atStartOfDay(PARIS).toInstant();
  private static final Instant COMMITMENT_END =
      LocalDate.of(2027, 10, 12).atStartOfDay(PARIS).toInstant();

  SubscriptionProductRepository subscriptionProductRepository = mock();
  UserRepository userRepository = mock();
  StripeUpgradeService stripeUpgradeService = mock();
  SubscriptionService subscriptionService = mock();
  SubscriptionUpgradeCommitmentService commitmentService = mock();
  SubscriptionUpgradeTransitionService transitionService = mock();
  UserSubscriptionUpgradeJpaRepository upgradeRepository = mock();
  TemporalUtils temporalUtils = mock();
  CreditGrantService creditGrantService = mock();
  EventProducer eventProducer = mock();
  SubscriptionUpgradeCompletionService subject =
      new SubscriptionUpgradeCompletionService(
          subscriptionProductRepository,
          userRepository,
          stripeUpgradeService,
          subscriptionService,
          commitmentService,
          transitionService,
          upgradeRepository,
          new SubscriptionUpgradeTimeline(temporalUtils),
          creditGrantService,
          eventProducer);

  User user = User.builder().id(USER_ID).userSubscriptionId(STRIPE_CUSTOMER_ID).build();
  SubscriptionProduct pro = SubscriptionProduct.builder().id("pro_id").name("Pro").build();

  @BeforeEach
  void setUp() {
    when(temporalUtils.startOfMonthAfter(LocalDate.of(2026, 10, 12)))
        .thenReturn(LocalDate.of(2026, 11, 1));
    when(subscriptionProductRepository.findById("pro_id")).thenReturn(Optional.of(pro));
    when(upgradeRepository.save(any(UserSubscriptionUpgrade.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(stripeUpgradeService.createUpgradedSubscription(
            anyString(), anyString(), any(), any(), anyLong(), anyString()))
        .thenReturn("sub_new");
    when(userRepository.getById(USER_ID)).thenReturn(user);
  }

  @Test
  void switches_the_subscription_from_the_dates_persisted_on_the_upgrade() {
    var paid = subject.complete(user, upgrade(MONTHLY), PAYMENT_METHOD_ID);

    assertEquals(PAID, paid.getStatus());
    assertEquals("sub_new", paid.getNewStripeSubscriptionId());
    var order = inOrder(stripeUpgradeService, subscriptionService, commitmentService);
    order
        .verify(stripeUpgradeService)
        .createUpgradedSubscription(
            STRIPE_CUSTOMER_ID,
            PAYMENT_METHOD_ID,
            pro,
            MONTHLY,
            LocalDate.of(2026, 11, 1).atStartOfDay(PARIS).toEpochSecond(),
            UPGRADE_ID);
    order.verify(subscriptionService).cancelSubscriptionImmediately("sub_old");
    order
        .verify(commitmentService)
        .switchTo(USER_ID, "pro_id", MONTHLY, UPGRADE_DATETIME, COMMITMENT_END);
    verify(creditGrantService).grantIncludedCredits(USER_ID, pro);
    var event = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(event.capture());
    assertEquals(
        UPGRADE_ID,
        ((SubscriptionUpgradeInvoicingRequested) event.getValue().getFirst())
            .getSubscriptionUpgradeId());
    verify(temporalUtils, never()).today();
  }

  @Test
  void anchors_a_yearly_upgrade_a_year_after_its_own_date() {
    subject.complete(user, upgrade(YEARLY), PAYMENT_METHOD_ID);

    verify(stripeUpgradeService)
        .createUpgradedSubscription(
            eq(STRIPE_CUSTOMER_ID),
            eq(PAYMENT_METHOD_ID),
            eq(pro),
            eq(YEARLY),
            eq(LocalDate.of(2027, 10, 12).atStartOfDay(PARIS).toEpochSecond()),
            eq(UPGRADE_ID));
  }

  @Test
  void keeps_the_upgrade_when_granting_the_credits_fails() {
    when(creditGrantService.grantIncludedCredits(USER_ID, pro))
        .thenThrow(new IllegalStateException("ledger down"));

    var paid = subject.complete(user, upgrade(MONTHLY), PAYMENT_METHOD_ID);

    assertEquals(PAID, paid.getStatus());
    verify(eventProducer).accept(any());
  }

  @Test
  void completes_an_upgrade_claimed_after_its_payment_on_the_payment_method_really_used() {
    when(transitionService.claimPaid(UPGRADE_ID, INVOICE_ID))
        .thenReturn(Optional.of(upgrade(MONTHLY)));
    when(stripeUpgradeService.resolveChargedPaymentMethod(STRIPE_CUSTOMER_ID, INVOICE_ID))
        .thenReturn("pm_new_card");

    var completed = subject.completeAfterPayment(UPGRADE_ID, INVOICE_ID);

    assertEquals(PAID, completed.orElseThrow().getStatus());
    verify(stripeUpgradeService)
        .createUpgradedSubscription(
            eq(STRIPE_CUSTOMER_ID), eq("pm_new_card"), eq(pro), eq(MONTHLY), anyLong(), any());
    verify(subscriptionService).cancelSubscriptionImmediately("sub_old");
  }

  @Test
  void does_nothing_when_the_upgrade_is_not_awaiting_that_payment() {
    when(transitionService.claimPaid(UPGRADE_ID, INVOICE_ID)).thenReturn(Optional.empty());

    assertTrue(subject.completeAfterPayment(UPGRADE_ID, INVOICE_ID).isEmpty());

    verifyNoInteractions(stripeUpgradeService, subscriptionService, commitmentService);
    verifyNoInteractions(creditGrantService, eventProducer);
  }

  @Test
  void leaves_the_claimed_upgrade_pending_when_the_switch_fails_after_the_payment() {
    when(transitionService.claimPaid(UPGRADE_ID, INVOICE_ID))
        .thenReturn(Optional.of(upgrade(MONTHLY)));
    when(stripeUpgradeService.resolveChargedPaymentMethod(STRIPE_CUSTOMER_ID, INVOICE_ID))
        .thenReturn(PAYMENT_METHOD_ID);
    when(stripeUpgradeService.createUpgradedSubscription(
            anyString(), anyString(), any(), any(), anyLong(), anyString()))
        .thenThrow(new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, "stripe down"));

    assertTrue(subject.completeAfterPayment(UPGRADE_ID, INVOICE_ID).isEmpty());

    verify(subscriptionService, never()).cancelSubscriptionImmediately(anyString());
    verify(commitmentService, never()).switchTo(any(), any(), any(), any(), any());
    verifyNoInteractions(creditGrantService, eventProducer);
  }

  @Test
  void leaves_the_claimed_upgrade_pending_when_its_target_plan_no_longer_exists() {
    when(transitionService.claimPaid(UPGRADE_ID, INVOICE_ID))
        .thenReturn(Optional.of(upgrade(MONTHLY)));
    when(stripeUpgradeService.resolveChargedPaymentMethod(STRIPE_CUSTOMER_ID, INVOICE_ID))
        .thenReturn(PAYMENT_METHOD_ID);
    when(subscriptionProductRepository.findById("pro_id")).thenReturn(Optional.empty());

    assertTrue(subject.completeAfterPayment(UPGRADE_ID, INVOICE_ID).isEmpty());

    verify(stripeUpgradeService, never())
        .createUpgradedSubscription(any(), any(), any(), any(), anyLong(), any());
  }

  private static UserSubscriptionUpgrade upgrade(BillingInterval toInterval) {
    return UserSubscriptionUpgrade.builder()
        .id(UPGRADE_ID)
        .userId(USER_ID)
        .fromSubscriptionProductId("essential_id")
        .toSubscriptionProductId("pro_id")
        .fromBillingInterval(MONTHLY)
        .toBillingInterval(toInterval)
        .upgradeDatetime(UPGRADE_DATETIME)
        .newCommitmentEndDatetime(COMMITMENT_END)
        .prorationAmountInCentsWithVat(3_871L)
        .stripeProrationInvoiceId(INVOICE_ID)
        .oldStripeSubscriptionId("sub_old")
        .status(PENDING)
        .build();
  }
}
