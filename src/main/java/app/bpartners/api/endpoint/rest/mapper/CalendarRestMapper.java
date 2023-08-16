package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CalendarEvent;
import app.bpartners.api.endpoint.rest.model.CreateCalendarEvent;
import org.springframework.stereotype.Component;

import static app.bpartners.api.repository.google.calendar.CalendarApi.zonedDateTimeFrom;

@Component
public class CalendarRestMapper {
  public CalendarEvent toRest(
      app.bpartners.api.model.CalendarEvent calendarEvent) {
    return new CalendarEvent()
        .id(calendarEvent.getId())
        .summary(calendarEvent.getSummary())
        .location(calendarEvent.getLocation())
        .organizer(calendarEvent.getOrganizer())
        .participants(calendarEvent.getParticipants())
        .from(calendarEvent.getFrom().toInstant())
        .to(calendarEvent.getTo().toInstant())
        .updatedAt(calendarEvent.getUpdatedAt());
  }

  public app.bpartners.api.model.CalendarEvent toDomain(CreateCalendarEvent rest) {
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
