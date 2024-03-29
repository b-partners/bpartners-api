package app.bpartners.api.service.utils;

import static app.bpartners.api.model.mapper.CalendarEventMapper.PARIS_TIMEZONE;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateUtils {
  private DateUtils() {}

  public static Date from_dd_MM_YYYY(String date) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    try {
      return date == null ? null : dateFormat.parse(date);
    } catch (ParseException e) {
      log.warn("Given date(" + date + ") does not follow format : dd-MM-YYYY");
      return null;
    }
  }

  public static String formatFrenchDatetime(Instant instant) {
    ZoneId zoneId = ZoneId.of(PARIS_TIMEZONE);
    LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    return localDateTime.format(formatter);
  }

  public static String formatFrenchDate(Instant instant) {
    ZoneId zoneId = ZoneId.of(PARIS_TIMEZONE);
    LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    return localDateTime.format(formatter);
  }

  public static String formatFrenchDateUnderscore(Instant instant) {
    ZoneId zoneId = ZoneId.of(PARIS_TIMEZONE);
    LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy");
    return localDateTime.format(formatter);
  }
}
