package app.bpartners.api.service.subscription;

import static java.math.RoundingMode.HALF_UP;

import app.bpartners.api.model.Fraction;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record ChargedFirstMonth(
    LocalDate start, LocalDate end, Fraction chargedWithoutVat, Fraction vatPercent) {
  private static final long BASIS_POINTS = 10_000L;
  private static final int SCALE = 10;

  public int days() {
    return (int) ChronoUnit.DAYS.between(start, end) + 1;
  }

  public Fraction creditedWithoutVat(int remainingDays) {
    return new Fraction(
        chargedWithoutVat.getNumerator().multiply(BigInteger.valueOf(remainingDays)),
        chargedWithoutVat.getDenominator().multiply(BigInteger.valueOf(days())));
  }

  public BigDecimal creditedInCentsWithVat(int remainingDays) {
    var credited = creditedWithoutVat(remainingDays);
    var withoutVat =
        new BigDecimal(credited.getNumerator())
            .divide(new BigDecimal(credited.getDenominator()), SCALE, HALF_UP);
    var vatRate =
        new BigDecimal(vatPercent.getNumerator())
            .divide(new BigDecimal(vatPercent.getDenominator()), SCALE, HALF_UP)
            .divide(BigDecimal.valueOf(BASIS_POINTS), SCALE, HALF_UP);
    return withoutVat.multiply(BigDecimal.ONE.add(vatRate));
  }
}
