package app.bpartners.api.service.subscription;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public record BillingMonthSegment(LocalDate start, LocalDate end, boolean fullMonth, int days) {

  public int monthLength() {
    return start.lengthOfMonth();
  }

  public static List<BillingMonthSegment> between(LocalDate periodStart, LocalDate periodEnd) {
    var segments = new ArrayList<BillingMonthSegment>();
    var cursor = periodStart;
    while (!cursor.isAfter(periodEnd)) {
      var monthEnd = cursor.withDayOfMonth(cursor.lengthOfMonth());
      var segmentEnd = monthEnd.isAfter(periodEnd) ? periodEnd : monthEnd;
      var fullMonth = cursor.getDayOfMonth() == 1 && segmentEnd.equals(monthEnd);
      var days = (int) ChronoUnit.DAYS.between(cursor, segmentEnd) + 1;
      segments.add(new BillingMonthSegment(cursor, segmentEnd, fullMonth, days));
      cursor = monthEnd.plusDays(1);
    }
    return segments;
  }
}
