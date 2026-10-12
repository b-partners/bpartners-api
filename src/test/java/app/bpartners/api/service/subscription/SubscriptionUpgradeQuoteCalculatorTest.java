package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import java.time.LocalDate;
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
}
