package app.bpartners.api.model.mapper;

import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.repository.jpa.model.HCalendarEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import static app.bpartners.api.repository.google.calendar.CalendarApi.dateTimeFrom;
import static app.bpartners.api.repository.google.calendar.CalendarApi.instantFrom;
import static app.bpartners.api.repository.google.calendar.CalendarApi.zonedDateTimeFrom;
import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class CalendarEventMapper {
  public static final String PARIS_TIMEZONE = "Europe/Paris";
  public static final String DEFAULT_EVENT_TYPE = "default";
  final ObjectMapper objectMapper = new ObjectMapper();

  public HCalendarEvent toEntity(String idUser, String idCalendar, Event event) {
    return HCalendarEvent.builder()
        .idUser(idUser)
        .idCalendar(idCalendar)
        .eteId(event.getId())
        .summary(event.getSummary())
        .location(event.getLocation())
        .organizer(event.getOrganizer().getEmail())
        .participants(encodeJsonList(getParticipants(event)))
        .from(event.getStart() == null ? null : instantFrom(event.getStart()))
        .to(event.getEnd() == null ? null : instantFrom(event.getEnd()))
        .updatedAt(instantFrom(event.getUpdated()))
        .build();
  }

  public Event toEvent(CalendarEvent event) {
    return new Event()
        .setEventType(DEFAULT_EVENT_TYPE)
        .setSummary(event.getSummary())
        .setOrganizer(new Event.Organizer().setEmail(event.getOrganizer()))
        .setCreator(new Event.Creator().setEmail(event.getOrganizer()))
        .setStart(new EventDateTime()
            .setDateTime(dateTimeFrom(event.getFrom().toInstant()))
            .setTimeZone(PARIS_TIMEZONE))
        .setEnd(new EventDateTime()
            .setDateTime(dateTimeFrom(event.getTo().toInstant()))
            .setTimeZone(PARIS_TIMEZONE))
        .setLocation(event.getLocation())
        .setAttendees(event.getParticipants() == null || event.getParticipants().isEmpty()
            ? List.of(new EventAttendee().setEmail(event.getOrganizer()))
            : event.getParticipants().stream()
            .map(participant -> new EventAttendee().setEmail(participant))
            .collect(Collectors.toList()))
        .setUpdated(dateTimeFrom(Instant.now()));
  }

  public CalendarEvent toCalendarEvent(Event event) {
    return CalendarEvent.builder()
        .id(String.valueOf(randomUUID())) //TODO: remove when calendar events are persisted
        .summary(event.getSummary())
        .location(event.getLocation())
        .organizer(event.getOrganizer().getEmail())
        .participants(getParticipants(event))
        .from(event.getStart() == null ? null : zonedDateTimeFrom(event.getStart()))
        .to(event.getEnd() == null ? null : zonedDateTimeFrom(event.getEnd()))
        .updatedAt(instantFrom(event.getUpdated()))
        .build();
  }

  private static List<String> getParticipants(Event event) {
    return event.getAttendees() != null && !event.getAttendees().isEmpty()
        ? event.getAttendees().stream()
        .map(EventAttendee::getEmail)
        .collect(Collectors.toList())
        : List.of();
  }

  public CalendarEvent toDomain(HCalendarEvent event) {
    return CalendarEvent.builder()
        .id(event.getId())
        .summary(event.getSummary())
        .location(event.getLocation())
        .organizer(event.getOrganizer())
        .participants(decodeJsonList(event.getParticipants()))
        .from(zonedDateTimeFrom(event.getFrom()))
        .to(zonedDateTimeFrom(event.getTo()))
        .updatedAt(event.getUpdatedAt())
        .build();
  }

  @SneakyThrows
  private String encodeJsonList(List<String> values) {
    return objectMapper.writeValueAsString(values);
  }

  @SneakyThrows
  private List<String> decodeJsonList(String value) {
    return objectMapper.readValue(value, new TypeReference<>() {
    });
  }
}
