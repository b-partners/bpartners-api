package app.bpartners.api.model.mapper;

import static app.bpartners.api.repository.google.calendar.CalendarApi.dateTimeFrom;
import static app.bpartners.api.repository.google.calendar.CalendarApi.instantFrom;
import static app.bpartners.api.repository.google.calendar.CalendarApi.zonedDateTimeFrom;
import static app.bpartners.api.service.utils.DataTypeUtils.decodeJsonList;
import static app.bpartners.api.service.utils.DataTypeUtils.encodeJsonList;

import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.repository.jpa.model.HCalendarEvent;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CalendarEventMapper {
  public static final String PARIS_TIMEZONE = "Europe/Paris";
  public static final String DEFAULT_EVENT_TYPE = "default";

  public HCalendarEvent toEntity(String id, String idUser, String idCalendar, Event googleEvent) {
    EventDateTime start = googleEvent.getStart();
    EventDateTime end = googleEvent.getEnd();
    return HCalendarEvent.builder()
        .id(id)
        .eteId(googleEvent.getId())
        .idUser(idUser)
        .idCalendar(idCalendar)
        .summary(googleEvent.getSummary())
        .location(googleEvent.getLocation())
        .organizer(googleEvent.getOrganizer().getEmail())
        .participants(encodeJsonList(getParticipants(googleEvent)))
        .from(start == null ? null : instantFrom(start))
        .to(end == null ? null : instantFrom(end))
        .updatedAt(instantFrom(googleEvent.getUpdated()))
        .build();
  }

  public HCalendarEvent toEntity(String idUser, String idCalendar, CalendarEvent domain) {
    return HCalendarEvent.builder()
        .id(domain.getId())
        .eteId(domain.getEteId()) // TODO: deprecated
        .idUser(idUser)
        .idCalendar(idCalendar)
        .summary(domain.getSummary())
        .location(domain.getLocation())
        .organizer(domain.getOrganizer())
        .participants(encodeJsonList(domain.getParticipants()))
        .from(domain.getFrom() == null ? null : domain.getFrom().toInstant())
        .to(domain.getTo() == null ? null : domain.getTo().toInstant())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }

  public Event toEvent(String id, CalendarEvent domain) {
    return new Event()
        .setId(id)
        .setEventType(DEFAULT_EVENT_TYPE)
        .setSummary(domain.getSummary())
        .setOrganizer(new Event.Organizer().setEmail(domain.getOrganizer()))
        .setCreator(new Event.Creator().setEmail(domain.getOrganizer()))
        .setStart(
            new EventDateTime()
                .setDateTime(dateTimeFrom(domain.getFrom().toInstant()))
                .setTimeZone(PARIS_TIMEZONE))
        .setEnd(
            new EventDateTime()
                .setDateTime(dateTimeFrom(domain.getTo().toInstant()))
                .setTimeZone(PARIS_TIMEZONE))
        .setLocation(domain.getLocation())
        .setAttendees(
            domain.getParticipants() == null || domain.getParticipants().isEmpty()
                ? List.of(new EventAttendee().setEmail(domain.getOrganizer()))
                : domain.getParticipants().stream()
                    .map(participant -> new EventAttendee().setEmail(participant))
                    .collect(Collectors.toList()))
        .setUpdated(dateTimeFrom(Instant.now()));
  }

  private static List<String> getParticipants(Event event) {
    return event.getAttendees() != null && !event.getAttendees().isEmpty()
        ? event.getAttendees().stream().map(EventAttendee::getEmail).collect(Collectors.toList())
        : List.of();
  }

  public CalendarEvent toDomain(HCalendarEvent entity) {
    return CalendarEvent.builder()
        .id(entity.getId())
        .summary(entity.getSummary())
        .location(entity.getLocation())
        .organizer(entity.getOrganizer())
        .participants(decodeJsonList(entity.getParticipants()))
        .from(zonedDateTimeFrom(entity.getFrom()))
        .to(zonedDateTimeFrom(entity.getTo()))
        .updatedAt(entity.getUpdatedAt())
        .sync(entity.isSync())
        .build();
  }

  public CalendarEvent toDomain(
      String id, boolean isSync, boolean isNewEvent, Instant updatedAt, Event googleEvent) {
    return CalendarEvent.builder()
        .id(id)
        .summary(googleEvent.getSummary())
        .location(googleEvent.getLocation())
        .organizer(googleEvent.getOrganizer().getEmail())
        .participants(getParticipants(googleEvent))
        .from(zonedDateTimeFrom(googleEvent.getStart()))
        .to(zonedDateTimeFrom(googleEvent.getEnd()))
        .updatedAt(updatedAt)
        .sync(isSync)
        .newEvent(isNewEvent)
        .eteId(googleEvent.getId()) // TODO: deprecated
        .build();
  }
}
