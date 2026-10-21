package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceNoVatWithDiscount;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Optional;
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
  void credit_lines_prorate_the_amount_actually_charged_on_the_first_month() {
    var charged =
        new ChargedFirstMonth(
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 31),
            new Fraction(BigInteger.valueOf(8_100L)),
            new Fraction(BigInteger.valueOf(2_000L)));

    var lines =
        subject.creditLines(
            "invoice_id",
            pro,
            MONTHLY,
            LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 11, 30),
            Optional.of(charged));

    assertEquals(2, lines.products().size());
    var first = lines.products().get(0).getUnitPrice();
    assertEquals(BigInteger.valueOf(-162_000L), first.getNumerator());
    assertEquals(BigInteger.valueOf(27L), first.getDenominator());
    assertEquals(
        BigInteger.valueOf(-10_000L), lines.products().get(1).getUnitPrice().getNumerator());
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

  @Test
  void labels_the_lines_with_a_generic_name_when_the_plan_has_no_name() {
    var unnamed = pro.toBuilder().name(" ").build();

    var lines =
        subject.newSubscriptionLines(
            "invoice_id", unnamed, MONTHLY, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

    assertTrue(lines.products().getFirst().getDescription().startsWith("Abonnement du"));
    var missingName = pro.toBuilder().name(null).build();
    assertTrue(
        subject
            .newSubscriptionLines(
                "invoice_id",
                missingName,
                MONTHLY,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30))
            .products()
            .getFirst()
            .getDescription()
            .startsWith("Abonnement du"));
  }

  @Test
  void applies_no_discount_when_the_annual_price_is_not_below_twelve_monthly_prices() {
    var noDiscount = pro.toBuilder().annualPriceInCentsWithVat(144_000L).build();

    var lines =
        subject.newSubscriptionLines(
            "invoice_id",
            noDiscount,
            YEARLY,
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2027, 10, 31));

    assertEquals(BigInteger.ZERO, lines.discountBasisPoints().getNumerator());
    assertEquals(12, lines.products().size());
    assertEquals(10_000.0, lines.products().getFirst().getUnitPrice().getApproximatedValue());
  }

  @Test
  void falls_back_on_the_default_vat_when_the_plan_declares_none() {
    var withoutVat = pro.toBuilder().vatPercent(null).build();

    var lines =
        subject.creditLines(
            "invoice_id",
            withoutVat,
            MONTHLY,
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30));

    assertEquals(
        BigInteger.valueOf(SubscriptionProduct.DEFAULT_VAT_PERCENT),
        lines.products().getFirst().getVatPercent().getNumerator());
  }
}
