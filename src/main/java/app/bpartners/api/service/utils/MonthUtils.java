package app.bpartners.api.service.utils;

import static java.time.LocalDate.now;

import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

@Component
public class MonthUtils {
  public LocalDate fifthOfNextMonth() {
    var today = now();
    var nextMonth = today.plusMonths(1);
    return nextMonth.withDayOfMonth(5);
  }

  public LocalDate startOfActualMonth() {
    var today = now();
    var currentMonth = YearMonth.from(today);
    return currentMonth.atDay(1);
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
}
