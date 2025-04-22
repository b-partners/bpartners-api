package app.bpartners.api.service.utils;

import static java.time.LocalDate.now;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class TemporalUtils {
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

  public Instant endOfMonth() {
    return LocalDate.now()
        .withDayOfMonth(LocalDate.now().lengthOfMonth())
        .atTime(23, 59, 59, 999_999_999)
        .atZone(ZoneId.of("Europe/Paris")) // Zone donnée
        .toInstant();
  }

  public Instant startOfMonth() {
    return LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.of("Europe/Paris")).toInstant();
  }
}
