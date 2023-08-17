package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.CalendarPermission;
import app.bpartners.api.model.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.CalendarPermission.OWNER;
import static app.bpartners.api.endpoint.rest.model.CalendarPermission.READER;
import static app.bpartners.api.endpoint.rest.model.CalendarPermission.UNKNOWN;
import static app.bpartners.api.endpoint.rest.model.CalendarPermission.WRITER;
import static java.util.UUID.randomUUID;

@Component
@Slf4j
public class CalendarMapper {

  public static final String OWNER_ROLE_VALUE = "owner";
  public static final String READER_ROLE_VALUE = "reader";
  public static final String WRITER_ROLE_VALUE = "writer";

  public Calendar toCalendar(CalendarListEntry calendar) {
    return Calendar.builder()
        .id(String.valueOf(randomUUID())) //TODO: replace when persisting
        .eteId(calendar.getId())
        .summary(calendar.getSummary())
        .calendarPermission(getPermission(calendar))
        .build();
  }

  public CalendarPermission getPermission(CalendarListEntry calendar) {
    switch (calendar.getAccessRole()) {
      case OWNER_ROLE_VALUE:
        return OWNER;
      case READER_ROLE_VALUE:
        return READER;
      case WRITER_ROLE_VALUE:
        return WRITER;
      default:
        log.warn("Unknown calendar role " + calendar.getAccessRole());
        return UNKNOWN;
    }
  }
}
