package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.UpgradeLineKind.CREDIT;
import static app.bpartners.api.model.subscription.UpgradeLineKind.NEW_CHARGE;
import static java.math.RoundingMode.HALF_UP;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UpgradeLineKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    return compute(
        fromProduct,
        fromInterval,
        toProduct,
        toInterval,
        upgradeDate,
        oldCoverageEnd,
        Optional.empty());
  }

  public UpgradeQuote compute(
      SubscriptionProduct fromProduct,
      BillingInterval fromInterval,
      SubscriptionProduct toProduct,
      BillingInterval toInterval,
      LocalDate upgradeDate,
      LocalDate oldCoverageEnd,
      Optional<ChargedFirstMonth> chargedFirstMonth) {
    if (fromInterval == YEARLY && toInterval == MONTHLY) {
      throw new NotImplementedException(
          "Upgrading from a yearly billed subscription to a monthly billed one is not supported"
              + " yet");
    }
    var newLines = newChargeLines(toProduct, toInterval, upgradeDate);
    var creditLines =
        creditLines(fromProduct, fromInterval, upgradeDate, oldCoverageEnd, chargedFirstMonth);
    var newCharge = totalOf(newLines);
    var credited = totalOf(creditLines);
    var lines = new ArrayList<UpgradeQuoteLine>();
    lines.addAll(roundedLines(NEW_CHARGE, toProduct, toInterval, newLines, newCharge));
    lines.addAll(roundedLines(CREDIT, fromProduct, fromInterval, creditLines, credited));
    var paid = paidPeriodOf(fromProduct, fromInterval, upgradeDate, oldCoverageEnd, chargedFirstMonth);
    return new UpgradeQuote(
        newCharge,
        credited,
        Math.max(newCharge - credited, 0L),
        paid.start(),
        paid.end(),
        paid.amountInCentsWithVat(),
        lines);
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

  private record PaidPeriod(LocalDate start, LocalDate end, long amountInCentsWithVat) {}

  private PaidPeriod paidPeriodOf(
      SubscriptionProduct fromProduct,
      BillingInterval fromInterval,
      LocalDate upgradeDate,
      LocalDate oldCoverageEnd,
      Optional<ChargedFirstMonth> chargedFirstMonth) {
    if (fromInterval == YEARLY) {
      var annual =
          monthlyPriceInCentsWithVat(fromProduct, YEARLY)
              .multiply(BigDecimal.valueOf(MONTHS_PER_YEAR))
              .setScale(0, HALF_UP)
              .longValueExact();
      return new PaidPeriod(oldCoverageEnd.minusYears(1).plusDays(1), oldCoverageEnd, annual);
    }
    if (chargedFirstMonth.isPresent()) {
      var charged = chargedFirstMonth.get();
      return new PaidPeriod(
          charged.start(),
          charged.end(),
          charged.creditedInCentsWithVat(charged.days()).setScale(0, HALF_UP).longValueExact());
    }
    var monthPrice =
        monthlyPriceInCentsWithVat(fromProduct, MONTHLY).setScale(0, HALF_UP).longValueExact();
    return new PaidPeriod(
        upgradeDate.withDayOfMonth(1), upgradeDate.withDayOfMonth(upgradeDate.lengthOfMonth()), monthPrice);
  }

  private record RawLine(
      LocalDate start,
      LocalDate end,
      int days,
      int referencePeriodDays,
      BigDecimal referencePriceInCentsWithVat,
      BigDecimal amountInCentsWithVat) {}

  private List<RawLine> newChargeLines(
      SubscriptionProduct toProduct, BillingInterval toInterval, LocalDate upgradeDate) {
    if (toInterval == YEARLY) {
      var yearEnd = upgradeDate.plusYears(1).minusDays(1);
      var days = (int) ChronoUnit.DAYS.between(upgradeDate, yearEnd) + 1;
      var annual =
          monthlyPriceInCentsWithVat(toProduct, YEARLY)
              .multiply(BigDecimal.valueOf(MONTHS_PER_YEAR))
              .setScale(0, HALF_UP);
      return List.of(new RawLine(upgradeDate, yearEnd, days, days, annual, annual));
    }
    return List.of(remainingOfMonthLine(toProduct, MONTHLY, upgradeDate));
  }

  private List<RawLine> creditLines(
      SubscriptionProduct fromProduct,
      BillingInterval fromInterval,
      LocalDate upgradeDate,
      LocalDate oldCoverageEnd,
      Optional<ChargedFirstMonth> chargedFirstMonth) {
    if (fromInterval == MONTHLY) {
      return List.of(
          chargedFirstMonth
              .map(charged -> remainingOfChargedMonthLine(charged, upgradeDate))
              .orElseGet(() -> remainingOfMonthLine(fromProduct, MONTHLY, upgradeDate)));
    }
    var monthly = monthlyPriceInCentsWithVat(fromProduct, YEARLY);
    return BillingMonthSegment.between(upgradeDate, oldCoverageEnd).stream()
        .map(segment -> segmentLine(monthly, segment))
        .toList();
  }

  private RawLine remainingOfChargedMonthLine(ChargedFirstMonth charged, LocalDate upgradeDate) {
    var remaining = BillingMonthSegment.between(upgradeDate, charged.end()).getFirst();
    return new RawLine(
        remaining.start(),
        remaining.end(),
        remaining.days(),
        charged.days(),
        charged.creditedInCentsWithVat(charged.days()),
        charged.creditedInCentsWithVat(remaining.days()));
  }

  private RawLine remainingOfMonthLine(
      SubscriptionProduct product, BillingInterval interval, LocalDate upgradeDate) {
    var monthEnd = upgradeDate.withDayOfMonth(upgradeDate.lengthOfMonth());
    var segment = BillingMonthSegment.between(upgradeDate, monthEnd).getFirst();
    return segmentLine(monthlyPriceInCentsWithVat(product, interval), segment);
  }

  private static RawLine segmentLine(BigDecimal monthlyPrice, BillingMonthSegment segment) {
    return new RawLine(
        segment.start(),
        segment.end(),
        segment.days(),
        segment.monthLength(),
        monthlyPrice,
        proratedOn(monthlyPrice, segment));
  }

  private static long totalOf(List<RawLine> lines) {
    return lines.stream()
        .map(RawLine::amountInCentsWithVat)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(0, HALF_UP)
        .longValueExact();
  }

  private static List<UpgradeQuoteLine> roundedLines(
      UpgradeLineKind kind,
      SubscriptionProduct product,
      BillingInterval interval,
      List<RawLine> lines,
      long total) {
    var rounded = new ArrayList<UpgradeQuoteLine>();
    var allocated = 0L;
    for (var index = 0; index < lines.size(); index++) {
      var line = lines.get(index);
      var last = index == lines.size() - 1;
      var amount =
          last
              ? total - allocated
              : line.amountInCentsWithVat().setScale(0, HALF_UP).longValueExact();
      allocated += amount;
      rounded.add(
          new UpgradeQuoteLine(
              kind,
              product.getId(),
              interval,
              line.start(),
              line.end(),
              line.days(),
              line.referencePeriodDays(),
              line.referencePriceInCentsWithVat().setScale(0, HALF_UP).longValueExact(),
              amount));
    }
    return rounded;
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
