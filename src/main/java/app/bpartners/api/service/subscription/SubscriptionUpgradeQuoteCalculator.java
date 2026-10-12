package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static java.math.RoundingMode.HALF_UP;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionUpgradeQuoteCalculator {
  private static final int MONTHS_PER_YEAR = 12;

  public UpgradeQuote compute(
      SubscriptionProduct fromProduct,
      BillingInterval fromInterval,
      SubscriptionProduct toProduct,
      BillingInterval toInterval,
      LocalDate upgradeDate,
      LocalDate oldCoverageEnd) {
    if (fromInterval == YEARLY && toInterval == MONTHLY) {
      throw new NotImplementedException(
          "Upgrading from a yearly billed subscription to a monthly billed one is not supported"
              + " yet");
    }
    var newCharge = newChargeInCents(toProduct, toInterval, upgradeDate);
    var credited = creditedInCents(fromProduct, fromInterval, upgradeDate, oldCoverageEnd);
    return new UpgradeQuote(newCharge, credited, Math.max(newCharge - credited, 0L));
  }

  public static BigDecimal monthlyPriceInCentsWithVat(
      SubscriptionProduct product, BillingInterval interval) {
    if (interval == YEARLY) {
      if (product.getAnnualPriceInCentsWithVat() == null) {
        throw new BadRequestException(
            "SubscriptionProduct(id=" + product.getId() + ") has no annual pricing");
      }
      return BigDecimal.valueOf(product.getAnnualPriceInCentsWithVat())
          .divide(BigDecimal.valueOf(MONTHS_PER_YEAR), 10, HALF_UP);
    }
    if (product.getPriceInCentsWithVat() == null) {
      throw new BadRequestException(
          "SubscriptionProduct(id=" + product.getId() + ") has no monthly pricing");
    }
    return BigDecimal.valueOf(product.getPriceInCentsWithVat());
  }

  private long newChargeInCents(
      SubscriptionProduct toProduct, BillingInterval toInterval, LocalDate upgradeDate) {
    if (toInterval == YEARLY) {
      return monthlyPriceInCentsWithVat(toProduct, YEARLY)
          .multiply(BigDecimal.valueOf(MONTHS_PER_YEAR))
          .setScale(0, HALF_UP)
          .longValueExact();
    }
    return remainingOfMonthInCents(toProduct, MONTHLY, upgradeDate)
        .setScale(0, HALF_UP)
        .longValueExact();
  }

  private long creditedInCents(
      SubscriptionProduct fromProduct,
      BillingInterval fromInterval,
      LocalDate upgradeDate,
      LocalDate oldCoverageEnd) {
    if (fromInterval == MONTHLY) {
      return remainingOfMonthInCents(fromProduct, MONTHLY, upgradeDate)
          .setScale(0, HALF_UP)
          .longValueExact();
    }
    var monthly = monthlyPriceInCentsWithVat(fromProduct, YEARLY);
    var total = BigDecimal.ZERO;
    for (var segment : BillingMonthSegment.between(upgradeDate, oldCoverageEnd)) {
      total = total.add(proratedOn(monthly, segment));
    }
    return total.setScale(0, HALF_UP).longValueExact();
  }

  private BigDecimal remainingOfMonthInCents(
      SubscriptionProduct product, BillingInterval interval, LocalDate upgradeDate) {
    var monthEnd = upgradeDate.withDayOfMonth(upgradeDate.lengthOfMonth());
    var segment = BillingMonthSegment.between(upgradeDate, monthEnd).getFirst();
    return proratedOn(monthlyPriceInCentsWithVat(product, interval), segment);
  }

  private static BigDecimal proratedOn(BigDecimal monthlyPrice, BillingMonthSegment segment) {
    if (segment.fullMonth()) {
      return monthlyPrice;
    }
    return monthlyPrice
        .multiply(BigDecimal.valueOf(segment.days()))
        .divide(BigDecimal.valueOf(segment.monthLength()), 10, HALF_UP);
  }
}
