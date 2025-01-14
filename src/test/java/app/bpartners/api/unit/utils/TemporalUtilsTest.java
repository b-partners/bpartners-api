package app.bpartners.api.unit.utils;

import static app.bpartners.api.service.utils.TemporalUtils.actualMonthValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TemporalUtilsTest {

  @Test
  void january_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(1);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Janvier", actual);
    localDateMockedStatic.close();
  }

  @Test
  void february_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(2);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Février", actual);
    localDateMockedStatic.close();
  }

  @Test
  void march_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(3);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Mars", actual);
    localDateMockedStatic.close();
  }

  @Test
  void april_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(4);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Avril", actual);
    localDateMockedStatic.close();
  }

  @Test
  void may_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(5);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Mai", actual);
    localDateMockedStatic.close();
  }

  @Test
  void june_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(6);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Juin", actual);
    localDateMockedStatic.close();
  }

  @Test
  void july_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(7);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Juillet", actual);
    localDateMockedStatic.close();
  }

  @Test
  void august_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(8);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Août", actual);
    localDateMockedStatic.close();
  }

  @Test
  void september_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(9);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Septembre", actual);
    localDateMockedStatic.close();
  }

  @Test
  void october_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(10);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Octobre", actual);
    localDateMockedStatic.close();
  }

  @Test
  void november_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(11);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Novembre", actual);
    localDateMockedStatic.close();
  }

  @Test
  void december_month_translated() {
    var localDateMockedStatic = mockStatic(LocalDate.class);
    var localDateMock = mock(LocalDate.class);
    when(localDateMock.getMonthValue()).thenReturn(12);
    localDateMockedStatic.when(LocalDate::now).thenReturn(localDateMock);

    var actual = actualMonthValue();

    assertEquals("Décembre", actual);
    localDateMockedStatic.close();
  }
}
