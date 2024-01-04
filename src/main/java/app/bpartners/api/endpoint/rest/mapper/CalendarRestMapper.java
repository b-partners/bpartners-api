package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.repository.google.calendar.CalendarApi.zonedDateTimeFrom;

import app.bpartners.api.endpoint.rest.model.Calendar;
import app.bpartners.api.endpoint.rest.model.CalendarEvent;
import app.bpartners.api.endpoint.rest.model.CreateCalendarEvent;
import app.bpartners.api.endpoint.rest.validator.CalendarEventValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CalendarRestMapper {
  private final CalendarEventValidator validator;

  public CalendarEvent toRest(app.bpartners.api.model.CalendarEvent domain) {
    return new CalendarEvent()
        .id(domain.getId())
        .summary(domain.getSummary())
        .location(domain.getLocation())
        .organizer(domain.getOrganizer())
        .participants(domain.getParticipants())
        .isSynchronized(domain.isSync())
        .from(domain.getFrom().toInstant())
        .to(domain.getTo().toInstant())
        .updatedAt(domain.getUpdatedAt());
  }

  public Calendar toRest(app.bpartners.api.model.Calendar calendar) {
    return new Calendar()
        .id(calendar.getId())
        .summary(calendar.getSummary())
        .permission(calendar.getCalendarPermission());
  }

  public app.bpartners.api.model.CalendarEvent toDomain(CreateCalendarEvent rest) {
    validator.accept(rest);
    return app.bpartners.api.model.CalendarEvent.builder()
        .id(rest.getId())
        .summary(rest.getSummary())
        .organizer(rest.getOrganizer())
        .location(rest.getLocation())
        .from(zonedDateTimeFrom(rest.getFrom()))
        .to(zonedDateTimeFrom(rest.getTo()))
        .participants(rest.getParticipants())
        .build();
  }
}
