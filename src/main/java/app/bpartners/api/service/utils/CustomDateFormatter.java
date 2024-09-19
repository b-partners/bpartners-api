package app.bpartners.api.service.utils;

import static app.bpartners.api.model.mapper.CalendarEventMapper.PARIS_TIMEZONE;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomDateFormatter {
  public LocalDate from_dd_MM_YYYY(String date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    try {
      return date == null ? null : LocalDate.parse(date, formatter);
    } catch (DateTimeParseException e) {
      log.warn("Given date(" + date + ") does not follow format : dd-MM-YYYY");
      return null;
    }
  }

  public String formatFrenchDatetime(Instant instant) {
    LocalDateTime localDateTime = instant.atZone(ZoneId.of(PARIS_TIMEZONE)).toLocalDateTime();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    return localDateTime.format(formatter);
  }

  public String formatFrenchDate(Instant instant) {
    ZoneId zoneId = ZoneId.of(PARIS_TIMEZONE);
    LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    return localDateTime.format(formatter);
  }

  public String formatFrenchDateUnderscore(Instant instant) {
    ZoneId zoneId = ZoneId.of(PARIS_TIMEZONE);
    LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy");
    return localDateTime.format(formatter);
  }
}
