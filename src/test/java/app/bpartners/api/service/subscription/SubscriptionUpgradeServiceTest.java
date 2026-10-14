package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.ACTIVE;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ConflictException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.utils.TemporalUtils;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  ChargedFirstMonthResolver chargedFirstMonthResolver = mock();
  UserSubscriptionUpgradeJpaRepository upgradeRepository = mock();
  SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository = mock();
  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();
  CreditGrantService creditGrantService = mock();
  EventProducer eventProducer = mock();
  TemporalUtils temporalUtils = mock();
  SubscriptionProductRepository subscriptionProductRepository = mock();
  UserRepository userRepository = mock();
  SubscriptionUpgradeTransitionService transitionService = mock();
  SubscriptionUpgradeExpirationService expirationService = mock();
  SubscriptionUpgradeTimeline timeline = new SubscriptionUpgradeTimeline(temporalUtils);
  SubscriptionUpgradeCompletionService completionService =
      new SubscriptionUpgradeCompletionService(
          subscriptionProductRepository,
          userRepository,
          stripeUpgradeService,
          subscriptionService,
          commitmentService,
          transitionService,
          upgradeRepository,
          timeline,
          creditGrantService,
          eventProducer);

  SubscriptionUpgradeService subject =
      new SubscriptionUpgradeService(
          subscriptionService,
          userSubscriptionProductService,
          stripeInvoiceService,
          stripeUpgradeService,
          new SubscriptionUpgradeQuoteCalculator(),
          commitmentService,
          chargedFirstMonthResolver,
          upgradeRepository,
          subscriptionInvoicePeriodRepository,
          subscriptionPaymentRepository,
          completionService,
          expirationService,
          timeline,
          temporalUtils);

  Map<String, SubscriptionProduct> catalog = new HashMap<>();
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
    catalog.put("essential_id", essential);
    catalog.put("pro_id", pro);
    when(subscriptionProductRepository.findById(anyString()))
        .thenAnswer(invocation -> Optional.ofNullable(catalog.get(invocation.getArgument(0))));
    when(subscriptionService.getByPlanId(anyString(), any(BillingInterval.class)))
        .thenAnswer(
            invocation ->
                Subscription.builder()
                    .subscriptionProduct(catalog.get(invocation.getArgument(0, String.class)))
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
        .thenReturn(new UpgradeCharge.Paid("in_proration"));
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
  void monthly_upgrade_credits_the_amount_actually_charged_on_the_first_partial_month() {
    activeProduct(essential, MONTHLY);
    when(chargedFirstMonthResolver.resolve(eq(USER_ID), eq(TODAY), any()))
        .thenReturn(Optional.of(chargedFirstMonth(4_100L)));

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(4_098L, upgrade.getProrationAmountInCentsWithVat());
    verify(stripeUpgradeService)
        .chargeOneOffAmount(
            eq(STRIPE_CUSTOMER_ID), eq(PAYMENT_METHOD_ID), eq(4_098L), anyString(), anyString());
  }

  @Test
  void monthly_to_yearly_upgrade_credits_the_amount_actually_charged_on_the_first_month() {
    activeProduct(essential, MONTHLY);
    when(chargedFirstMonthResolver.resolve(eq(USER_ID), eq(TODAY), any()))
        .thenReturn(Optional.of(chargedFirstMonth(4_100L)));

    var upgrade = subject.upgrade(user, "pro_id", YEARLY);

    assertEquals(104_356L, upgrade.getProrationAmountInCentsWithVat());
  }

  @Test
  void monthly_upgrade_without_a_charged_first_month_keeps_the_day_based_credit() {
    activeProduct(essential, MONTHLY);
    when(chargedFirstMonthResolver.resolve(eq(USER_ID), eq(TODAY), any()))
        .thenReturn(Optional.empty());

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(3_871L, upgrade.getProrationAmountInCentsWithVat());
  }

  @Test
  void a_charged_first_month_larger_than_the_new_charge_leaves_nothing_to_pay() {
    activeProduct(essential, MONTHLY);
    when(chargedFirstMonthResolver.resolve(eq(USER_ID), eq(TODAY), any()))
        .thenReturn(Optional.of(chargedFirstMonth(40_000L)));

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(0L, upgrade.getProrationAmountInCentsWithVat());
    verify(stripeUpgradeService, never())
        .chargeOneOffAmount(anyString(), anyString(), anyLong(), anyString(), anyString());
  }

  private static ChargedFirstMonth chargedFirstMonth(long chargedInCentsWithoutVat) {
    return new ChargedFirstMonth(
        LocalDate.of(2026, 10, 5),
        LocalDate.of(2026, 10, 31),
        new Fraction(BigInteger.valueOf(chargedInCentsWithoutVat)),
        new Fraction(BigInteger.valueOf(2_000L)));
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
  void awaits_the_customer_payment_without_switching_anything_when_the_charge_needs_an_action() {
    activeProduct(essential, MONTHLY);
    when(stripeUpgradeService.chargeOneOffAmount(
            anyString(), anyString(), anyLong(), anyString(), anyString()))
        .thenReturn(new UpgradeCharge.ActionRequired("in_open", "https://pay.example/in_open"));

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(AWAITING_PAYMENT, upgrade.getStatus());
    assertEquals("in_open", upgrade.getStripeProrationInvoiceId());
    assertEquals("https://pay.example/in_open", upgrade.getPaymentUrl());
    assertEquals(OLD_STRIPE_SUBSCRIPTION_ID, upgrade.getOldStripeSubscriptionId());
    assertEquals(timeline.paymentExpirationOf(upgrade), upgrade.getPaymentExpirationDatetime());
    assertFalse(upgrade.getPaymentExpirationDatetime().isBefore(upgrade.getCreationDatetime()));
    verify(upgradeRepository, never()).delete(any(UserSubscriptionUpgrade.class));
    verify(stripeUpgradeService, never())
        .createUpgradedSubscription(any(), any(), any(), any(), anyLong(), any());
    verify(subscriptionService, never()).cancelSubscriptionImmediately(anyString());
    verify(commitmentService, never()).switchTo(any(), any(), any(), any(), any());
    verifyNoInteractions(creditGrantService, eventProducer);
  }

  @Test
  void refuses_an_upgrade_while_another_one_awaits_its_payment() {
    activeProduct(essential, MONTHLY);
    when(upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(USER_ID))
        .thenReturn(
            List.of(
                UserSubscriptionUpgrade.builder()
                    .id("awaiting_id")
                    .status(AWAITING_PAYMENT)
                    .build()));

    assertThrows(ConflictException.class, () -> subject.upgrade(user, "pro_id", null));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void lets_the_user_upgrade_again_once_the_awaited_payment_has_expired() {
    activeProduct(essential, MONTHLY);
    var overdue =
        UserSubscriptionUpgrade.builder().id("overdue_id").status(AWAITING_PAYMENT).build();
    when(upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(USER_ID))
        .thenReturn(List.of(overdue));
    when(expirationService.expireIfOverdue(overdue)).thenReturn(true);

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(PAID, upgrade.getStatus());
  }

  @Test
  void checks_the_upgrades_in_progress_before_the_unpaid_invoices() {
    activeProduct(essential, MONTHLY);
    when(upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(USER_ID))
        .thenReturn(
            List.of(
                UserSubscriptionUpgrade.builder()
                    .id("awaiting_id")
                    .status(AWAITING_PAYMENT)
                    .build()));
    when(stripeInvoiceService.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(List.of(new com.stripe.model.Invoice()));

    assertThrows(ConflictException.class, () -> subject.upgrade(user, "pro_id", null));
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

  @Test
  void refuses_a_user_without_stripe_customer() {
    var userWithoutStripe = User.builder().id(USER_ID).build();

    assertThrows(
        BadRequestException.class, () -> subject.upgrade(userWithoutStripe, "pro_id", null));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void refuses_a_target_plan_without_tier_rank() {
    activeProduct(essential, MONTHLY);
    catalog.put("unranked_id", product("unranked_id", null, 20_000L, 240_000L));

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "unranked_id", null));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void considers_a_legacy_plan_without_tier_rank_as_the_lowest_one() {
    activeProduct(product("legacy_id", null, 1_000L, 12_000L), MONTHLY);

    var upgrade = subject.upgrade(user, "essential_id", null);

    assertEquals(PAID, upgrade.getStatus());
  }

  @Test
  void refuses_the_same_tier_billed_yearly_on_both_sides() {
    activeProduct(pro, YEARLY);

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "pro_id", YEARLY));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void refuses_an_upgrade_without_stripe_subscription() {
    activeProduct(essential, MONTHLY);
    when(subscriptionService.getSubscriptionByUser(user))
        .thenReturn(UserSubscription.builder().user(user).subscriptions(List.of()).build());

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "pro_id", null));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void refuses_an_upgrade_when_the_stripe_subscription_is_not_active() {
    activeProduct(essential, MONTHLY);
    when(subscriptionService.getSubscriptionByUser(user))
        .thenReturn(
            UserSubscription.builder()
                .user(user)
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .e2Id(OLD_STRIPE_SUBSCRIPTION_ID)
                            .status(Subscription.SubscriptionStatus.CANCELED)
                            .startDatetime(Instant.parse("2026-09-01T00:00:00Z"))
                            .build()))
                .build());

    assertThrows(BadRequestException.class, () -> subject.upgrade(user, "pro_id", null));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void bills_monthly_when_the_active_product_has_no_billing_interval_but_is_not_a_trial() {
    activeProduct(essential, null);

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(MONTHLY, upgrade.getFromBillingInterval());
    assertEquals(MONTHLY, upgrade.getToBillingInterval());
  }

  @Test
  void charges_nothing_when_the_new_plan_costs_less_than_the_remaining_value() {
    activeProduct(product("premium_id", 1, 100_000L, 1_200_000L), MONTHLY);
    catalog.put("cheap_top_id", product("cheap_top_id", 2, 100L, 1_200L));

    var upgrade = subject.upgrade(user, "cheap_top_id", null);

    assertEquals(PAID, upgrade.getStatus());
    assertEquals(0L, upgrade.getProrationAmountInCentsWithVat());
    assertNull(upgrade.getStripeProrationInvoiceId());
    verify(stripeUpgradeService, never())
        .chargeOneOffAmount(anyString(), anyString(), anyLong(), anyString(), anyString());
  }

  @Test
  void keeps_the_upgrade_when_granting_the_credits_fails() {
    activeProduct(essential, MONTHLY);
    when(creditGrantService.grantIncludedCredits(USER_ID, pro))
        .thenThrow(new IllegalStateException("ledger down"));

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(PAID, upgrade.getStatus());
    verify(eventProducer).accept(any());
  }

  @Test
  void covers_the_old_contract_until_the_latest_invoiced_period_ignoring_unusable_ones() {
    activeProduct(essential, MONTHLY);
    when(subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId(USER_ID))
        .thenReturn(List.of("refunded_invoice"));
    when(subscriptionInvoicePeriodRepository.findByUserId(USER_ID))
        .thenReturn(
            List.of(
                period("refunded_invoice", "2027-12-31"),
                period("no_end_invoice", null),
                period("past_invoice", "2026-09-30"),
                period("early_invoice", "2027-03-31"),
                period("latest_invoice", "2027-09-30")));

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(
        LocalDate.of(2027, 9, 30).atStartOfDay(PARIS).toInstant(),
        upgrade.getOldCoverageEndDatetime());
  }

  @Test
  void covers_the_old_contract_until_the_last_full_month_of_its_commitment_without_invoice() {
    activeProduct(essential, MONTHLY);
    when(commitmentService.findActiveCommitment(USER_ID))
        .thenReturn(Optional.of(commitmentEndingOn("2027-10-15")));

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(
        LocalDate.of(2027, 9, 30).atStartOfDay(PARIS).toInstant(),
        upgrade.getOldCoverageEndDatetime());
  }

  @Test
  void covers_the_old_contract_until_the_end_of_the_month_when_its_commitment_is_over() {
    activeProduct(essential, MONTHLY);
    when(commitmentService.findActiveCommitment(USER_ID))
        .thenReturn(Optional.of(commitmentEndingOn("2026-10-01")));

    var upgrade = subject.upgrade(user, "pro_id", null);

    assertEquals(
        LocalDate.of(2026, 10, 31).atStartOfDay(PARIS).toInstant(),
        upgrade.getOldCoverageEndDatetime());
  }

  private static SubscriptionInvoicePeriod period(String invoiceId, String endDate) {
    return SubscriptionInvoicePeriod.builder()
        .id("period_" + invoiceId)
        .userId(USER_ID)
        .invoiceId(invoiceId)
        .periodEndDatetime(
            endDate == null ? null : LocalDate.parse(endDate).atStartOfDay(PARIS).toInstant())
        .build();
  }

  private static UserSubscriptionCommitment commitmentEndingOn(String endDate) {
    return UserSubscriptionCommitment.builder()
        .id("commitment_id")
        .userId(USER_ID)
        .commitmentEndDatetime(LocalDate.parse(endDate).atStartOfDay(PARIS).toInstant())
        .build();
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
      String id, Integer tierRank, long priceInCentsWithoutVat, long annualPriceInCentsWithVat) {
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
