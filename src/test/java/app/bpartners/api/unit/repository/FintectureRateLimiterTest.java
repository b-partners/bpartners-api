package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.repository.fintecture.implementation.utils.FintectureRateLimiter;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FintectureRateLimiterTest {

  @BeforeEach
  void resetLimiter() throws Exception {
    var lastCallField = FintectureRateLimiter.class.getDeclaredField("lastCallTime");
    lastCallField.setAccessible(true);
    var ref = (AtomicReference<LocalDateTime>) lastCallField.get(null);
    ref.set(null);
  }

  @Test
  void first_call_is_allowed() {
    assertTrue(FintectureRateLimiter.canCall());
  }

  @Test
  void second_call_within_one_hour_is_denied() {
    assertTrue(FintectureRateLimiter.canCall());
    assertFalse(FintectureRateLimiter.canCall());
  }

  @Test
  void call_after_one_hour_is_allowed() throws Exception {
    assertTrue(FintectureRateLimiter.canCall());

    var lastCallField = FintectureRateLimiter.class.getDeclaredField("lastCallTime");
    lastCallField.setAccessible(true);
    var ref = (AtomicReference<LocalDateTime>) lastCallField.get(null);
    ref.set(LocalDateTime.now().minusMinutes(65));

    assertTrue(FintectureRateLimiter.canCall());
  }

  @Test
  void minutes_to_wait_is_correct() throws Exception {
    assertTrue(FintectureRateLimiter.canCall());

    Field lastCallField = FintectureRateLimiter.class.getDeclaredField("lastCallTime");
    lastCallField.setAccessible(true);
    AtomicReference<LocalDateTime> ref = (AtomicReference<LocalDateTime>) lastCallField.get(null);
    ref.set(LocalDateTime.now().minusMinutes(30));

    long minutes = FintectureRateLimiter.minutesToWait();
    assertTrue(minutes <= 30 && minutes >= 29);
  }
}
