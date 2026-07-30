package app.bpartners.api.service.utils;

import static java.time.LocalDate.now;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class TemporalUtils {
  private static final ZoneId ZONE_ID_OF_EUROPE_PARIS = ZoneId.of("Europe/Paris");

  public LocalDate fifthOfNextMonth() {
    return fifthOfMonthAfter(1);
  }

  public LocalDate startOfNextMonth() {
    var today = now();
    var nextMonth = today.plusMonths(1);
    return nextMonth.withDayOfMonth(1);
  }

  public LocalDate fourthOfNextMonth() {
    var today = now();
    var nextMonth = today.plusMonths(1);
    return nextMonth.withDayOfMonth(4);
  }

  public LocalDate fifthOfMonthAfter(int monthsOffset) {
    var today = now();
    var nextMonth = today.plusMonths(monthsOffset);
    return nextMonth.withDayOfMonth(5);
  }

  public LocalDate startOfActualMonth() {
    var today = now();
    var currentMonth = YearMonth.from(today);
    return currentMonth.atDay(1);
  }

  public LocalDate fifthOfActualMonth() {
    var today = now();
    var currentMonth = YearMonth.from(today);
    return currentMonth.atDay(5);
  }

  public LocalDate fifthOfLastMonth() {
    var today = now();
    var lastMonth = YearMonth.from(today).minusMonths(1);
    return lastMonth.atDay(5);
  }

  public LocalDate startOfLastMonth() {
    var today = now();
    var lastMonth = YearMonth.from(today).minusMonths(1);
    return lastMonth.atDay(1);
  }

  public LocalDate endOfLastMonth() {
    var today = now();
    var lastMonth = YearMonth.from(today).minusMonths(1);
    return lastMonth.atEndOfMonth();
  }

  public LocalDate endOfActualMonth() {
    var today = now();
    var currentMonth = YearMonth.from(today);
    return currentMonth.atEndOfMonth();
  }

  public static String actualMonthValue() {
    LocalDate today = LocalDate.now();
    return switch (today.getMonthValue()) {
      case 1 -> "Janvier";
      case 2 -> "Février";
      case 3 -> "Mars";
      case 4 -> "Avril";
      case 5 -> "Mai";
      case 6 -> "Juin";
      case 7 -> "Juillet";
      case 8 -> "Août";
      case 9 -> "Septembre";
      case 10 -> "Octobre";
      case 11 -> "Novembre";
      case 12 -> "Décembre";
      default -> throw new IllegalArgumentException("Invalid month value " + today.getMonthValue());
    };
  }

  public LocalDate getLastDayOfInstant(Instant t) {
    ZoneId zone = ZONE_ID_OF_EUROPE_PARIS;
    return t.atZone(zone)
        .toLocalDate()
        .withDayOfMonth(t.atZone(zone).toLocalDate().lengthOfMonth());
  }

  public Instant endOfMonth() {
    return LocalDate.now()
        .withDayOfMonth(LocalDate.now().lengthOfMonth())
        .atTime(23, 59, 59, 999_999_999)
        .atZone(ZONE_ID_OF_EUROPE_PARIS) // Zone donnée
        .toInstant();
  }

  public Instant startOfMonth() {
    return LocalDate.now().withDayOfMonth(1).atStartOfDay(ZONE_ID_OF_EUROPE_PARIS).toInstant();
  }

  public Instant startOfLastMonthInstant() {
    return startOfLastMonth().atStartOfDay(ZONE_ID_OF_EUROPE_PARIS).toInstant();
  }

  public Instant endOfLastMonthInstant() {
    return endOfLastMonth()
        .atTime(23, 59, 59, 999_999_999)
        .atZone(ZONE_ID_OF_EUROPE_PARIS)
        .toInstant();
  }

  public Instant getSixthOfMonthAt2359(Instant instant, int plusMonth) {
    return instant
        .atZone(ZONE_ID_OF_EUROPE_PARIS)
        .plusMonths(plusMonth)
        .withDayOfMonth(6)
        .withHour(23)
        .withMinute(59)
        .withSecond(0)
        .withNano(0)
        .toInstant();
  }

  public Instant getSixthOfNextMonthAt2359(Instant instant) {
    return instant
        .atZone(ZONE_ID_OF_EUROPE_PARIS)
        .plusMonths(1)
        .withDayOfMonth(6)
        .withHour(23)
        .withMinute(59)
        .withSecond(0)
        .withNano(0)
        .toInstant();
  }
}
