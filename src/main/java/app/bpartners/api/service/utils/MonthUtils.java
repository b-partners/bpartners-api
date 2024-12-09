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
}
