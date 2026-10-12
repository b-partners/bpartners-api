package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.ACTIVE;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ConflictException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
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

class SubscriptionUpgradeServiceTest {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final String USER_ID = "subscriber_id";
  private static final String STRIPE_CUSTOMER_ID = "cus_subscriber";
  private static final String OLD_STRIPE_SUBSCRIPTION_ID = "sub_old";
  private static final String PAYMENT_METHOD_ID = "pm_default";
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

  SubscriptionService subscriptionService = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  StripeInvoiceService stripeInvoiceService = mock();
  StripeUpgradeService stripeUpgradeService = mock();
  SubscriptionUpgradeCommitmentService commitmentService = mock();
  UserSubscriptionUpgradeJpaRepository upgradeRepository = mock();
  SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository = mock();
  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();
  CreditGrantService creditGrantService = mock();
  EventProducer eventProducer = mock();
  TemporalUtils temporalUtils = mock();

  SubscriptionUpgradeService subject =
      new SubscriptionUpgradeService(
          subscriptionService,
          userSubscriptionProductService,
          stripeInvoiceService,
          stripeUpgradeService,
          new SubscriptionUpgradeQuoteCalculator(),
          commitmentService,
          upgradeRepository,
          subscriptionInvoicePeriodRepository,
          subscriptionPaymentRepository,
          creditGrantService,
          eventProducer,
          temporalUtils);

  SubscriptionProduct essential = product("essential_id", 1, 5_000L, 54_000L);
  SubscriptionProduct pro = product("pro_id", 2, 10_000L, 108_000L);
  User user = User.builder().id(USER_ID).userSubscriptionId(STRIPE_CUSTOMER_ID).build();

  @BeforeEach
  void setUp() {
    when(temporalUtils.today()).thenReturn(TODAY);
    when(temporalUtils.startOfMonthAfter(TODAY)).thenReturn(LocalDate.of(2026, 11, 1));
    when(upgradeRepository.save(any(UserSubscriptionUpgrade.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(USER_ID))
        .thenReturn(List.of());
    when(stripeInvoiceService.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID)).thenReturn(List.of());
    when(subscriptionService.getByPlanId(anyString(), any(BillingInterval.class)))
        .thenAnswer(
            invocation ->
                Subscription.builder()
                    .subscriptionProduct(
                        "pro_id".equals(invocation.getArgument(0)) ? pro : essential)
                    .build());
    when(subscriptionService.getSubscriptionByUser(user))
        .thenReturn(
            UserSubscription.builder()
                .user(user)
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .e2Id(OLD_STRIPE_SUBSCRIPTION_ID)
                            .status(ACTIVE)
                            .startDatetime(Instant.parse("2026-09-01T00:00:00Z"))
                            .build()))
                .build());
    when(stripeUpgradeService.requireDefaultPaymentMethod(STRIPE_CUSTOMER_ID))
        .thenReturn(PAYMENT_METHOD_ID);
    when(stripeUpgradeService.chargeOneOffAmount(
            eq(STRIPE_CUSTOMER_ID), eq(PAYMENT_METHOD_ID), anyLong(), anyString(), anyString()))
        .thenReturn("in_proration");
    when(stripeUpgradeService.createUpgradedSubscription(
            eq(STRIPE_CUSTOMER_ID), eq(PAYMENT_METHOD_ID), any(), any(), anyLong(), anyString()))
        .thenReturn("sub_new");
    when(commitmentService.findActiveCommitment(USER_ID)).thenReturn(Optional.empty());
    when(subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId(USER_ID))
        .thenReturn(List.of());
    when(subscriptionInvoicePeriodRepository.findByUserId(USER_ID)).thenReturn(List.of());
  }

  @Test
  void upgrades_monthly_to_monthly_charges_the_difference_and_starts_payments_next_month() {
    activeProduct(essential, MONTHLY);

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(PAID, upgrade.getStatus());
    assertEquals(3_871L, upgrade.getProrationAmountInCentsWithVat());
    assertEquals("in_proration", upgrade.getStripeProrationInvoiceId());
    assertEquals("sub_new", upgrade.getNewStripeSubscriptionId());
    assertEquals(MONTHLY, upgrade.getToBillingInterval());
    verify(stripeUpgradeService)
        .chargeOneOffAmount(
            eq(STRIPE_CUSTOMER_ID), eq(PAYMENT_METHOD_ID), eq(3_871L), anyString(), anyString());
    var anchor = ArgumentCaptor.forClass(Long.class);
    verify(stripeUpgradeService)
        .createUpgradedSubscription(
            eq(STRIPE_CUSTOMER_ID),
            eq(PAYMENT_METHOD_ID),
            eq(pro),
            eq(MONTHLY),
            anchor.capture(),
            anyString());
    assertEquals(LocalDate.of(2026, 11, 1).atStartOfDay(PARIS).toEpochSecond(), anchor.getValue());
    verify(subscriptionService).cancelSubscriptionImmediately(OLD_STRIPE_SUBSCRIPTION_ID);
    verify(commitmentService)
        .switchTo(
            USER_ID,
            "pro_id",
            MONTHLY,
            TODAY.atStartOfDay(PARIS).toInstant(),
            TODAY.plusYears(1).atStartOfDay(PARIS).toInstant());
    verify(creditGrantService).grantIncludedCredits(USER_ID, pro);
    verify(eventProducer).accept(any());
  }

  @Test
  void upgrades_monthly_to_yearly_on_the_same_tier_and_anchors_the_next_payment_a_year_later() {
    activeProduct(pro, MONTHLY);

    var upgrade = subject.upgrade(user, "pro_id", YEARLY);

    assertEquals(YEARLY, upgrade.getToBillingInterval());
    assertEquals(100_258L, upgrade.getProrationAmountInCentsWithVat());
    var anchor = ArgumentCaptor.forClass(Long.class);
    verify(stripeUpgradeService)
        .createUpgradedSubscription(
            eq(STRIPE_CUSTOMER_ID),
            eq(PAYMENT_METHOD_ID),
            eq(pro),
            eq(YEARLY),
            anchor.capture(),
            anyString());
    assertEquals(TODAY.plusYears(1).atStartOfDay(PARIS).toEpochSecond(), anchor.getValue());
  }

  @Test
  void refuses_a_downgrade_without_touching_stripe() {
    activeProduct(pro, MONTHLY);

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "essential_id", null));

    verifyNoInteractions(stripeUpgradeService);
    verify(subscriptionService, never()).cancelSubscriptionImmediately(anyString());
  }

  @Test
  void refuses_the_same_tier_without_a_move_to_yearly() {
    activeProduct(pro, MONTHLY);

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "pro_id", MONTHLY));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void refuses_an_upgrade_during_a_free_trial() {
    when(userSubscriptionProductService.findActiveUserSubscriptionProduct(USER_ID))
        .thenReturn(
            Optional.of(
                UserSubscriptionProduct.builder()
                    .userId(USER_ID)
                    .subscriptionProduct(essential)
                    .billingInterval(null)
                    .subscriptionEndDatetime(Instant.parse("2026-10-20T00:00:00Z"))
                    .build()));

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "pro_id", null));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void refuses_an_upgrade_without_active_subscription() {
    when(userSubscriptionProductService.findActiveUserSubscriptionProduct(USER_ID))
        .thenReturn(Optional.empty());

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "pro_id", null));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void yearly_to_monthly_is_not_implemented_and_nothing_is_charged() {
    activeProduct(essential, YEARLY);
    when(subscriptionInvoicePeriodRepository.findByUserId(USER_ID)).thenReturn(List.of());

    assertThrows(NotImplementedException.class, () -> subject.upgrade(user, "pro_id", MONTHLY));

    verify(stripeUpgradeService, never())
        .chargeOneOffAmount(anyString(), anyString(), anyLong(), anyString(), anyString());
    verify(subscriptionService, never()).cancelSubscriptionImmediately(anyString());
  }

  @Test
  void refuses_an_upgrade_while_another_one_is_pending() {
    activeProduct(essential, MONTHLY);
    when(upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(USER_ID))
        .thenReturn(
            List.of(UserSubscriptionUpgrade.builder().id("pending_id").status(PENDING).build()));

    assertThrows(ConflictException.class, () -> subject.upgrade(user, "pro_id", null));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void a_failed_charge_leaves_the_subscription_untouched_and_drops_the_pending_upgrade() {
    activeProduct(essential, MONTHLY);
    when(stripeUpgradeService.chargeOneOffAmount(
            anyString(), anyString(), anyLong(), anyString(), anyString()))
        .thenThrow(new BadRequestException("card declined"));

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "pro_id", null));

    verify(upgradeRepository).delete(any(UserSubscriptionUpgrade.class));
    verify(stripeUpgradeService, never())
        .createUpgradedSubscription(any(), any(), any(), any(), anyLong(), any());
    verify(subscriptionService, never()).cancelSubscriptionImmediately(anyString());
    verify(commitmentService, never()).switchTo(any(), any(), any(), any(), any());
    verifyNoInteractions(creditGrantService, eventProducer);
  }

  @Test
  void refuses_an_upgrade_with_unpaid_invoices() {
    activeProduct(essential, MONTHLY);
    when(stripeInvoiceService.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(List.of(new com.stripe.model.Invoice()));

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "pro_id", null));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void finds_the_upgrades_of_the_user_filtered_on_status() {
    var pending = UserSubscriptionUpgrade.builder().id("pending_id").status(PENDING).build();
    var paid = UserSubscriptionUpgrade.builder().id("paid_id").status(PAID).build();
    when(upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(USER_ID))
        .thenReturn(List.of(pending, paid));

    assertEquals(List.of(pending, paid), subject.findUpgrades(USER_ID, null));
    assertEquals(List.of(pending), subject.findUpgrades(USER_ID, PENDING));
  }

  private void activeProduct(SubscriptionProduct product, BillingInterval interval) {
    when(userSubscriptionProductService.findActiveUserSubscriptionProduct(USER_ID))
        .thenReturn(
            Optional.of(
                UserSubscriptionProduct.builder()
                    .userId(USER_ID)
                    .subscriptionProduct(product)
                    .billingInterval(interval)
                    .subscriptionStartDatetime(Instant.parse("2026-09-01T00:00:00Z"))
                    .build()));
  }

  private static SubscriptionProduct product(
      String id, int tierRank, long priceInCentsWithoutVat, long annualPriceInCentsWithVat) {
    return SubscriptionProduct.builder()
        .id(id)
        .name(id)
        .tierRank(tierRank)
        .vatPercent(2_000L)
        .priceInCentsWithoutVat(priceInCentsWithoutVat)
        .annualPriceInCentsWithVat(annualPriceInCentsWithVat)
        .build();
  }
}
