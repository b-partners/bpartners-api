package app.bpartners.api.unit.utils;

import static app.bpartners.api.service.utils.TemporalUtils.actualMonthValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.service.utils.TemporalUtils;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TemporalUtilsTest {
  private static final ZoneId ZONE_ID_OF_EUROPE_PARIS = ZoneId.of("Europe/Paris");
  private final TemporalUtils subject = new TemporalUtils();

  @Test
  void start_of_last_month_instant_is_first_day_at_paris_midnight() {
    var lastMonth = YearMonth.from(LocalDate.now()).minusMonths(1);
    var expected = lastMonth.atDay(1).atStartOfDay(ZONE_ID_OF_EUROPE_PARIS).toInstant();

    assertEquals(expected, subject.startOfLastMonthInstant());
  }

  @Test
  void end_of_last_month_instant_is_last_day_at_paris_end_of_day() {
    var lastMonth = YearMonth.from(LocalDate.now()).minusMonths(1);
    var expected =
        lastMonth
            .atEndOfMonth()
            .atTime(23, 59, 59, 999_999_999)
            .atZone(ZONE_ID_OF_EUROPE_PARIS)
            .toInstant();

    assertEquals(expected, subject.endOfLastMonthInstant());
  }

  @Test
  void last_month_instant_window_starts_before_it_ends() {
    org.junit.jupiter.api.Assertions.assertTrue(
        subject.startOfLastMonthInstant().isBefore(subject.endOfLastMonthInstant()));
  }

  @Test
  void january_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(1);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Janvier", actual);
    localDateMockedStatic.close();
  }

  @Test
  void february_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(2);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Février", actual);
    localDateMockedStatic.close();
  }

  @Test
  void march_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(3);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Mars", actual);
    localDateMockedStatic.close();
  }

  @Test
  void april_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(4);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Avril", actual);
    localDateMockedStatic.close();
  }

  @Test
  void may_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(5);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Mai", actual);
    localDateMockedStatic.close();
  }

  @Test
  void june_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(6);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Juin", actual);
    localDateMockedStatic.close();
  }

  @Test
  void july_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(7);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Juillet", actual);
    localDateMockedStatic.close();
  }

  @Test
  void august_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(8);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Août", actual);
    localDateMockedStatic.close();
  }

  @Test
  void september_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(9);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Septembre", actual);
    localDateMockedStatic.close();
  }

  @Test
  void october_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(10);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Octobre", actual);
    localDateMockedStatic.close();
  }

  @Test
  void november_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(11);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Novembre", actual);
    localDateMockedStatic.close();
  }

  @Test
  void december_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(12);
    localDateMockedStatic
        .when(() -> LocalDate.now(ZONE_ID_OF_EUROPE_PARIS))
        .thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Décembre", actual);
    localDateMockedStatic.close();
  }
}
