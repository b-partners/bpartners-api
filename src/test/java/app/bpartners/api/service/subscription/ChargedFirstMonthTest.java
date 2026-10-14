package app.bpartners.api.service.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.model.Fraction;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ChargedFirstMonthTest {
  ChargedFirstMonth subject =
      new ChargedFirstMonth(
          LocalDate.of(2026, 10, 5),
          LocalDate.of(2026, 10, 31),
          new Fraction(BigInteger.valueOf(4_100L)),
          new Fraction(BigInteger.valueOf(2_000L)));

  @Test
  void counts_the_days_of_the_charged_segment() {
    assertEquals(27, subject.days());
  }

  @Test
  void credits_the_remaining_days_pro_rata_of_the_charged_amount() {
    assertEquals(3_037, Math.round(subject.creditedWithoutVat(20).getApproximatedValue()));
    assertEquals(4_100, Math.round(subject.creditedWithoutVat(27).getApproximatedValue()));
  }

  @Test
  void adds_the_vat_on_the_credited_amount() {
    assertEquals(
        3_644L,
        subject.creditedInCentsWithVat(20).setScale(0, RoundingMode.HALF_UP).longValueExact());
  }

  @Test
  void credits_nothing_when_no_day_remains() {
    assertEquals(0L, subject.creditedInCentsWithVat(0).longValueExact());
  }

  @Test
  void credits_the_whole_charged_amount_with_vat_when_every_day_remains() {
    assertEquals(
        4_920L, subject.creditedInCentsWithVat(27).setScale(0, RoundingMode.HALF_UP).longValue());
  }

  @Test
  void adds_no_vat_when_the_charged_line_has_none() {
    var withoutVat =
        new ChargedFirstMonth(
            subject.start(),
            subject.end(),
            subject.chargedWithoutVat(),
            new Fraction(BigInteger.ZERO));

    assertEquals(
        4_100L,
        withoutVat.creditedInCentsWithVat(27).setScale(0, RoundingMode.HALF_UP).longValue());
  }

  @Test
  void counts_the_days_of_a_segment_in_a_leap_february() {
    var february =
        new ChargedFirstMonth(
            LocalDate.of(2028, 2, 10),
            LocalDate.of(2028, 2, 29),
            new Fraction(BigInteger.valueOf(2_000L)),
            new Fraction(BigInteger.valueOf(2_000L)));

    assertEquals(20, february.days());
  }
}
