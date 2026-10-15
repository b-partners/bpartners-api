package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UpgradeLineKind;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubscriptionUpgradeQuoteCalculatorTest {
  private static final LocalDate UPGRADE_DATE = LocalDate.of(2026, 10, 12);
  private static final LocalDate OLD_MONTHLY_COVERAGE_END = LocalDate.of(2027, 9, 30);
  private static final LocalDate OLD_YEARLY_COVERAGE_END = LocalDate.of(2027, 3, 31);

  SubscriptionUpgradeQuoteCalculator subject = new SubscriptionUpgradeQuoteCalculator();

  SubscriptionProduct essential = product("essential_id", 5_000L, 54_000L);
  SubscriptionProduct pro = product("pro_id", 10_000L, 108_000L);

  @Test
  void monthly_to_monthly_charges_the_difference_on_the_remaining_days_of_the_month() {
    var quote =
        subject.compute(essential, MONTHLY, pro, MONTHLY, UPGRADE_DATE, OLD_MONTHLY_COVERAGE_END);

    assertEquals(7_742L, quote.newChargeInCentsWithVat());
    assertEquals(3_871L, quote.creditedInCentsWithVat());
    assertEquals(3_871L, quote.amountDueInCentsWithVat());
  }

  @Test
  void details_the_periods_behind_a_monthly_to_monthly_quote() {
    var quote =
        subject.compute(essential, MONTHLY, pro, MONTHLY, UPGRADE_DATE, OLD_MONTHLY_COVERAGE_END);

    var newCharge = quote.lines().get(0);
    assertEquals(UpgradeLineKind.NEW_CHARGE, newCharge.kind());
    assertEquals("pro_id", newCharge.subscriptionProductId());
    assertEquals(LocalDate.of(2026, 10, 12), newCharge.periodStart());
    assertEquals(LocalDate.of(2026, 10, 31), newCharge.periodEnd());
    assertEquals(20, newCharge.days());
    assertEquals(31, newCharge.referencePeriodDays());
    assertEquals(12_000L, newCharge.referencePriceInCentsWithVat());
    assertEquals(7_742L, newCharge.amountInCentsWithVat());
    var credit = quote.lines().get(1);
    assertEquals(UpgradeLineKind.CREDIT, credit.kind());
    assertEquals("essential_id", credit.subscriptionProductId());
    assertEquals(6_000L, credit.referencePriceInCentsWithVat());
    assertEquals(3_871L, credit.amountInCentsWithVat());
  }

  @Test
  void identifies_the_month_already_paid_for_a_monthly_subscription() {
    var quote =
        subject.compute(essential, MONTHLY, pro, MONTHLY, UPGRADE_DATE, OLD_MONTHLY_COVERAGE_END);

    assertEquals(LocalDate.of(2026, 10, 1), quote.paidPeriodStart());
    assertEquals(LocalDate.of(2026, 10, 31), quote.paidPeriodEnd());
    assertEquals(6_000L, quote.paidAmountInCentsWithVat());
  }

  @Test
  void identifies_the_first_partial_month_actually_invoiced() {
    var charged =
        new ChargedFirstMonth(
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 31),
            new Fraction(BigInteger.valueOf(4_100L)),
            new Fraction(BigInteger.valueOf(2_000L)));

    var quote =
        subject.compute(
            essential,
            MONTHLY,
            pro,
            MONTHLY,
            UPGRADE_DATE,
            OLD_MONTHLY_COVERAGE_END,
            Optional.of(charged));

    assertEquals(LocalDate.of(2026, 10, 5), quote.paidPeriodStart());
    assertEquals(LocalDate.of(2026, 10, 31), quote.paidPeriodEnd());
    assertEquals(4_920L, quote.paidAmountInCentsWithVat());
  }

  @Test
  void identifies_the_year_already_paid_for_a_yearly_subscription() {
    var quote =
        subject.compute(essential, YEARLY, pro, YEARLY, UPGRADE_DATE, OLD_YEARLY_COVERAGE_END);

    assertEquals(LocalDate.of(2026, 4, 1), quote.paidPeriodStart());
    assertEquals(OLD_YEARLY_COVERAGE_END, quote.paidPeriodEnd());
    assertEquals(54_000L, quote.paidAmountInCentsWithVat());
  }

  @Test
  void details_one_credit_line_per_remaining_month_of_a_yearly_subscription() {
    var quote =
        subject.compute(essential, YEARLY, pro, YEARLY, UPGRADE_DATE, OLD_YEARLY_COVERAGE_END);

    var newCharge = quote.lines().get(0);
    assertEquals(LocalDate.of(2027, 10, 11), newCharge.periodEnd());
    assertEquals(newCharge.days(), newCharge.referencePeriodDays());
    assertEquals(quote.newChargeInCentsWithVat(), newCharge.amountInCentsWithVat());
    var credits = quote.lines().stream().filter(l -> l.kind() == UpgradeLineKind.CREDIT).toList();
    assertEquals(6, credits.size());
    assertEquals(
        quote.creditedInCentsWithVat(),
        credits.stream().mapToLong(UpgradeQuoteLine::amountInCentsWithVat).sum());
  }

  @Test
  void monthly_credit_follows_the_amount_actually_charged_on_the_first_partial_month() {
    var charged =
        new ChargedFirstMonth(
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 31),
            new Fraction(BigInteger.valueOf(4_100L)),
            new Fraction(BigInteger.valueOf(2_000L)));

    var quote =
        subject.compute(
            essential,
            MONTHLY,
            pro,
            MONTHLY,
            UPGRADE_DATE,
            OLD_MONTHLY_COVERAGE_END,
            Optional.of(charged));

    assertEquals(7_742L, quote.newChargeInCentsWithVat());
    assertEquals(3_644L, quote.creditedInCentsWithVat());
    assertEquals(4_098L, quote.amountDueInCentsWithVat());
  }

  @Test
  void upgrade_on_the_day_of_the_subscription_credits_exactly_what_stripe_charged() {
    var proPlan = product("pro_plan_id", 9_900L, 106_920L);
    var expertPlan = product("expert_plan_id", 19_900L, 214_920L);
    var charged =
        new ChargedFirstMonth(
            LocalDate.of(2026, 10, 14),
            LocalDate.of(2026, 10, 31),
            new Fraction(BigInteger.valueOf(5_632L)),
            new Fraction(BigInteger.valueOf(2_000L)));

    var quote =
        subject.compute(
            proPlan,
            MONTHLY,
            expertPlan,
            MONTHLY,
            LocalDate.of(2026, 10, 14),
            LocalDate.of(2027, 9, 30),
            Optional.of(charged));

    assertEquals(13_866L, quote.newChargeInCentsWithVat());
    assertEquals(6_758L, quote.creditedInCentsWithVat());
    assertEquals(7_108L, quote.amountDueInCentsWithVat());
  }

  @Test
  void yearly_credit_ignores_a_charged_first_month() {
    var charged =
        new ChargedFirstMonth(
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 31),
            new Fraction(BigInteger.valueOf(4_100L)),
            new Fraction(BigInteger.valueOf(2_000L)));

    var quote =
        subject.compute(
            essential,
            YEARLY,
            pro,
            YEARLY,
            UPGRADE_DATE,
            OLD_YEARLY_COVERAGE_END,
            Optional.of(charged));

    assertEquals(25_403L, quote.creditedInCentsWithVat());
  }

  @Test
  void monthly_to_monthly_on_the_first_of_the_month_charges_the_full_month_difference() {
    var quote =
        subject.compute(
            essential, MONTHLY, pro, MONTHLY, LocalDate.of(2026, 11, 1), OLD_MONTHLY_COVERAGE_END);

    assertEquals(6_000L, quote.amountDueInCentsWithVat());
  }

  @Test
  void monthly_to_yearly_charges_the_renewed_year_minus_the_remaining_month() {
    var quote =
        subject.compute(essential, MONTHLY, pro, YEARLY, UPGRADE_DATE, OLD_MONTHLY_COVERAGE_END);

    assertEquals(108_000L, quote.newChargeInCentsWithVat());
    assertEquals(3_871L, quote.creditedInCentsWithVat());
    assertEquals(104_129L, quote.amountDueInCentsWithVat());
  }

  @Test
  void yearly_to_yearly_charges_the_renewed_year_minus_the_remaining_period() {
    var quote =
        subject.compute(essential, YEARLY, pro, YEARLY, UPGRADE_DATE, OLD_YEARLY_COVERAGE_END);

    assertEquals(108_000L, quote.newChargeInCentsWithVat());
    assertEquals(25_403L, quote.creditedInCentsWithVat());
    assertEquals(82_597L, quote.amountDueInCentsWithVat());
  }

  @Test
  void yearly_to_monthly_is_not_implemented() {
    assertThrows(
        NotImplementedException.class,
        () ->
            subject.compute(
                essential, YEARLY, pro, MONTHLY, UPGRADE_DATE, OLD_YEARLY_COVERAGE_END));
  }

  @Test
  void february_remaining_days_are_prorated_on_the_length_of_february() {
    var quote =
        subject.compute(
            essential, MONTHLY, pro, MONTHLY, LocalDate.of(2027, 2, 15), OLD_MONTHLY_COVERAGE_END);

    assertEquals(3_000L, quote.amountDueInCentsWithVat());
  }

  private static SubscriptionProduct product(
      String id, long priceInCentsWithoutVat, long annualPriceInCentsWithVat) {
    return SubscriptionProduct.builder()
        .id(id)
        .vatPercent(2_000L)
        .priceInCentsWithoutVat(priceInCentsWithoutVat)
        .annualPriceInCentsWithVat(annualPriceInCentsWithVat)
        .build();
  }

  @Test
  void refuses_a_yearly_plan_without_annual_pricing() {
    var withoutAnnualPrice =
        product("monthly_only_id", 5_000L, 0L).toBuilder().annualPriceInCentsWithVat(null).build();

    assertThrows(
        BadRequestException.class,
        () ->
            subject.compute(
                essential,
                MONTHLY,
                withoutAnnualPrice,
                YEARLY,
                UPGRADE_DATE,
                OLD_MONTHLY_COVERAGE_END));
  }

  @Test
  void refuses_a_monthly_plan_without_monthly_pricing() {
    var withoutMonthlyPrice =
        product("no_price_id", 0L, 54_000L).toBuilder().priceInCentsWithoutVat(null).build();

    assertThrows(
        BadRequestException.class,
        () ->
            subject.compute(
                withoutMonthlyPrice,
                MONTHLY,
                pro,
                MONTHLY,
                UPGRADE_DATE,
                OLD_MONTHLY_COVERAGE_END));
  }
}
