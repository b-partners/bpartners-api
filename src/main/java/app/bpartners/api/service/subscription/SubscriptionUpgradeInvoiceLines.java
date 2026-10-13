package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.ProductStatus.ENABLED;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionUpgradeInvoiceLines {
  private static final int MONTHS_PER_YEAR = 12;
  private static final long BASIS_POINTS = 10_000L;

  private final CustomDateFormatter customDateFormatter;

  public record Lines(List<InvoiceProduct> products, Fraction discountBasisPoints) {}

  public Lines creditLines(
      String invoiceIdentifier,
      SubscriptionProduct product,
      BillingInterval interval,
      LocalDate from,
      LocalDate to) {
    var pricing = pricingOf(product, interval);
    var products = new ArrayList<InvoiceProduct>();
    for (var segment : BillingMonthSegment.between(from, to)) {
      var unitPrice = negate(prorated(pricing.monthlyUnitPrice(), segment));
      products.add(
          line(
              invoiceIdentifier,
              "Avoir " + label(product, segment.start(), segment.end()),
              unitPrice,
              vatPercentOf(product)));
    }
    return new Lines(products, pricing.discountBasisPoints());
  }

  public Lines newSubscriptionLines(
      String invoiceIdentifier,
      SubscriptionProduct product,
      BillingInterval interval,
      LocalDate from,
      LocalDate to) {
    var pricing = pricingOf(product, interval);
    var segments = BillingMonthSegment.between(from, to);
    var products = new ArrayList<InvoiceProduct>();
    var allocated = new Fraction(BigInteger.ZERO);
    var yearlyGrossTarget =
        pricing
            .monthlyUnitPrice()
            .operate(new Fraction(BigInteger.valueOf(MONTHS_PER_YEAR)), Aprational::multiply);
    for (var index = 0; index < segments.size(); index++) {
      var segment = segments.get(index);
      var lastYearlySegment = interval == YEARLY && index == segments.size() - 1;
      var unitPrice =
          lastYearlySegment
              ? yearlyGrossTarget.operate(allocated, Aprational::subtract)
              : prorated(pricing.monthlyUnitPrice(), segment);
      allocated = allocated.operate(unitPrice, Aprational::add);
      products.add(
          line(
              invoiceIdentifier,
              label(product, segment.start(), segment.end()),
              unitPrice,
              vatPercentOf(product)));
    }
    return new Lines(products, pricing.discountBasisPoints());
  }

  private String label(SubscriptionProduct product, LocalDate start, LocalDate end) {
    var name = product.getName();
    var prefix = name == null || name.isBlank() ? "Abonnement" : "Abonnement " + name;
    return prefix
        + " du "
        + customDateFormatter.formatFrenchDate(start)
        + " au "
        + customDateFormatter.formatFrenchDate(end);
  }

  private record Pricing(Fraction monthlyUnitPrice, Fraction discountBasisPoints) {}

  private Pricing pricingOf(SubscriptionProduct product, BillingInterval interval) {
    var listMonthly = BigInteger.valueOf(product.getPriceInCentsWithoutVat());
    if (interval != YEARLY) {
      return new Pricing(new Fraction(listMonthly), new Fraction(BigInteger.ZERO));
    }
    var netAnnual = BigInteger.valueOf(product.getAnnualPriceInCentsWithoutVat());
    var grossAnnual = listMonthly.multiply(BigInteger.valueOf(MONTHS_PER_YEAR));
    if (grossAnnual.compareTo(netAnnual) <= 0) {
      return new Pricing(
          new Fraction(netAnnual, BigInteger.valueOf(MONTHS_PER_YEAR)),
          new Fraction(BigInteger.ZERO));
    }
    var discount =
        new Fraction(
            BigInteger.valueOf(BASIS_POINTS).multiply(grossAnnual.subtract(netAnnual)),
            grossAnnual);
    return new Pricing(new Fraction(listMonthly), discount);
  }

  private static Fraction prorated(Fraction monthlyUnitPrice, BillingMonthSegment segment) {
    if (segment.fullMonth()) {
      return monthlyUnitPrice;
    }
    return new Fraction(
        monthlyUnitPrice.getNumerator().multiply(BigInteger.valueOf(segment.days())),
        monthlyUnitPrice.getDenominator().multiply(BigInteger.valueOf(segment.monthLength())));
  }

  private static Fraction negate(Fraction fraction) {
    return new Fraction(fraction.getNumerator().negate(), fraction.getDenominator());
  }

  private static Fraction vatPercentOf(SubscriptionProduct product) {
    var vatPercent =
        product.getVatPercent() == null
            ? SubscriptionProduct.DEFAULT_VAT_PERCENT
            : product.getVatPercent();
    return new Fraction(BigInteger.valueOf(vatPercent));
  }

  private static InvoiceProduct line(
      String invoiceIdentifier, String description, Fraction unitPrice, Fraction vatPercent) {
    return InvoiceProduct.builder()
        .id(randomUUID().toString())
        .idInvoice(invoiceIdentifier)
        .createdAt(now())
        .description(description)
        .quantity(1)
        .unitPrice(unitPrice)
        .vatPercent(vatPercent)
        .status(ENABLED)
        .build();
  }
}
