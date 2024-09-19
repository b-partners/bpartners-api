package app.bpartners.api.unit.date;

import static app.bpartners.api.model.mapper.CalendarEventMapper.PARIS_TIMEZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CustomDateFormatterTest {
  CustomDateFormatter subject = new CustomDateFormatter();

  @Test
  void from_dd_MM_YYY_ok() {
    var expected =
        Instant.parse("2024-01-01T00:00:00Z").atZone(ZoneId.of(PARIS_TIMEZONE)).toLocalDate();

    var actual = subject.from_dd_MM_YYYY("01/01/2024");

    assertEquals(expected, actual);
  }

  @Test
  void from_dd_MM_YYY_ko() {
    assertNull(subject.from_dd_MM_YYYY("01/0/2024"));
    assertNull(subject.from_dd_MM_YYYY("01-01/2024"));
    assertNull(subject.from_dd_MM_YYYY("01-01-2024"));
  }
}
