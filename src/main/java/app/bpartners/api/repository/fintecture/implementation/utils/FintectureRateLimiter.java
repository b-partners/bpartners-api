package app.bpartners.api.repository.fintecture.implementation.utils;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

public class FintectureRateLimiter {
  private static final AtomicReference<LocalDateTime> lastCallTime = new AtomicReference<>(null);
  private static final int HOURS_INTERVAL = 1;

  public static boolean canCall() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime last = lastCallTime.get();

    if (last == null || last.plusHours(HOURS_INTERVAL).isBefore(now)) {
      lastCallTime.set(now);
      return true;
    }
    return false;
  }

  public static long minutesToWait() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime last = lastCallTime.get();
    if (last == null) return 0;
    return java.time.Duration.between(now, last.plusHours(HOURS_INTERVAL)).toMinutes();
  }
}
