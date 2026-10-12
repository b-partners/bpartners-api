package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceNoVatWithDiscount;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SubscriptionUpgradeInvoiceLinesTest {
  SubscriptionUpgradeInvoiceLines subject =
      new SubscriptionUpgradeInvoiceLines(new CustomDateFormatter());

  SubscriptionProduct pro =
      SubscriptionProduct.builder()
          .id("pro_id")
          .name("Pro")
          .vatPercent(2_000L)
          .priceInCentsWithoutVat(10_000L)
          .annualPriceInCentsWithVat(108_000L)
          .build();

  @Test
  void credit_lines_are_negative_one_per_calendar_month_with_a_prorated_first_month() {
    var lines =
        subject.creditLines(
            "invoice_id", pro, MONTHLY, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 12, 31));

    assertEquals(3, lines.products().size());
    assertEquals(
        BigInteger.valueOf(-200_000L), lines.products().get(0).getUnitPrice().getNumerator());
    assertEquals(BigInteger.valueOf(31L), lines.products().get(0).getUnitPrice().getDenominator());
    assertEquals(
        BigInteger.valueOf(-10_000L), lines.products().get(1).getUnitPrice().getNumerator());
    assertTrue(lines.products().get(0).getDescription().startsWith("Avoir Abonnement Pro du"));
    assertTrue(
        computePriceNoVatWithDiscount(lines.discountBasisPoints(), lines.products())
                .getApproximatedValue()
            < 0);
  }

  @Test
  void yearly_new_subscription_lines_cover_twelve_months_and_total_the_annual_price() {
    var lines =
        subject.newSubscriptionLines(
            "invoice_id", pro, YEARLY, LocalDate.of(2026, 10, 12), LocalDate.of(2027, 10, 11));

    assertEquals(13, lines.products().size());
    var totalWithoutVat =
        computePriceNoVatWithDiscount(lines.discountBasisPoints(), lines.products());
    assertEquals(90_000L, Math.round(totalWithoutVat.getApproximatedValue()));
  }

  @Test
  void monthly_new_subscription_lines_start_with_the_prorated_switching_month() {
    var lines =
        subject.newSubscriptionLines(
            "invoice_id", pro, MONTHLY, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 30));

    assertEquals(2, lines.products().size());
    assertEquals(
        BigInteger.valueOf(200_000L), lines.products().get(0).getUnitPrice().getNumerator());
    assertEquals(
        BigInteger.valueOf(10_000L), lines.products().get(1).getUnitPrice().getNumerator());
  }
}
