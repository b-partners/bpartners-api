package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CalendarEvent;
import org.springframework.stereotype.Component;

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
}
