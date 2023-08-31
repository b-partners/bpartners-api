package app.bpartners.api.service.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateUtils {
  private DateUtils() {
  }

  public static Date from_dd_MM_YYYY(String date) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    try {
      return date == null ? null : dateFormat.parse(date);
    } catch (ParseException e) {
      log.warn("Given date(" + date + ") does not follow format : dd-MM-YYYY");
      return null;
    }
  }
}
