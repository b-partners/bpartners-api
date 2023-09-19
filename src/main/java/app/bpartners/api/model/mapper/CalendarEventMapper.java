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

@Component
@AllArgsConstructor
public class CalendarEventMapper {
  public static final String PARIS_TIMEZONE = "Europe/Paris";
  public static final String DEFAULT_EVENT_TYPE = "default";
  final ObjectMapper objectMapper = new ObjectMapper();

  public HCalendarEvent toEntity(String id, String idUser, String idCalendar, Event event) {
    EventDateTime start = event.getStart();
    EventDateTime end = event.getEnd();
    return HCalendarEvent.builder()
        .id(id)
        .eteId(event.getId())
        .idUser(idUser)
        .idCalendar(idCalendar)
        .summary(event.getSummary())
        .location(event.getLocation())
        .organizer(event.getOrganizer().getEmail())
        .participants(encodeJsonList(getParticipants(event)))
        .from(start == null ? null : instantFrom(start))
        .to(end == null ? null : instantFrom(end))
        .updatedAt(instantFrom(event.getUpdated()))
        .build();
  }

  public HCalendarEvent toEntity(String idUser, String idCalendar, CalendarEvent domain) {
    return HCalendarEvent.builder()
        .id(domain.getId())
        .eteId(null)
        .sync(false)
        .idUser(idUser)
        .idCalendar(idCalendar)
        .summary(domain.getSummary())
        .location(domain.getLocation())
        .organizer(domain.getOrganizer())
        .participants(encodeJsonList(domain.getParticipants()))
        .from(domain.getFrom() == null ? null : domain.getFrom().toInstant())
        .to(domain.getTo() == null ? null : domain.getTo().toInstant())
        .updatedAt(Instant.now())
        .build();
  }

  public Event toEvent(String id, CalendarEvent domain) {
    return new Event()
        .setId(id)
        .setEventType(DEFAULT_EVENT_TYPE)
        .setSummary(domain.getSummary())
        .setOrganizer(new Event.Organizer().setEmail(domain.getOrganizer()))
        .setCreator(new Event.Creator().setEmail(domain.getOrganizer()))
        .setStart(new EventDateTime()
            .setDateTime(dateTimeFrom(domain.getFrom().toInstant()))
            .setTimeZone(PARIS_TIMEZONE))
        .setEnd(new EventDateTime()
            .setDateTime(dateTimeFrom(domain.getTo().toInstant()))
            .setTimeZone(PARIS_TIMEZONE))
        .setLocation(domain.getLocation())
        .setAttendees(domain.getParticipants() == null || domain.getParticipants().isEmpty()
            ? List.of(new EventAttendee().setEmail(domain.getOrganizer()))
            : domain.getParticipants().stream()
            .map(participant -> new EventAttendee().setEmail(participant))
            .collect(Collectors.toList()))
        .setUpdated(dateTimeFrom(Instant.now()));
  }

  private static List<String> getParticipants(Event event) {
    return event.getAttendees() != null && !event.getAttendees().isEmpty()
        ? event.getAttendees().stream()
        .map(EventAttendee::getEmail)
        .collect(Collectors.toList())
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
