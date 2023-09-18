package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Calendar;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.mapper.CalendarEventMapper;
import app.bpartners.api.repository.CalendarEventRepository;
import app.bpartners.api.repository.CalendarRepository;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.jpa.CalendarEventJpaRepository;
import app.bpartners.api.repository.jpa.model.HCalendarEvent;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.repository.google.calendar.CalendarApi.dateTimeFrom;
import static java.util.UUID.randomUUID;

@Repository
@AllArgsConstructor
public class CalendarEventRepositoryImpl implements CalendarEventRepository {
  private final CalendarApi calendarApi;
  private final CalendarEventMapper eventMapper;
  private final CalendarRepository calendarRepository;
  private final CalendarEventJpaRepository jpaRepository;


  @Override
  public List<CalendarEvent> findByIdUserAndIdCalendar(String idUser,
                                                       String idCalendar,
                                                       Instant from,
                                                       Instant to) {
    Calendar calendar = calendarRepository.getById(idCalendar);
    DateTime dateMin = dateTimeFrom(from);
    DateTime dateMax = dateTimeFrom(to);

    List<Event> eventEntries;
    try {
      eventEntries =
          calendarApi.getEvents(idUser, calendar.getEteId(), dateMin, dateMax);
    } catch (Exception e) {
      eventEntries = List.of();
    }
    List<HCalendarEvent> retrievedEvents = jpaRepository.findByIdUser(idUser);

    List<HCalendarEvent> eventEntities =
        eventEntries.stream()
            .map(event -> eventMapper.toEntity(idUser, idCalendar, event))
            .collect(Collectors.toList());
    for (HCalendarEvent newEntity : eventEntities) {
      newEntity.setId(String.valueOf(randomUUID()));
      newEntity.setNewEvent(true);
      newEntity.setCreatedAt(Instant.now());
      for (HCalendarEvent actualEvent : retrievedEvents) {
        if (actualEvent.getEteId().equals(newEntity.getEteId())) {
          newEntity.setNewEvent(false);
          break;
        }
      }
    }

    List<HCalendarEvent> createdEvents = eventEntities.isEmpty() ? List.of()
        : jpaRepository.saveAll(eventEntities.stream()
        .filter(HCalendarEvent::isNewEvent)
        .collect(Collectors.toList()));
    List<HCalendarEvent> all = createdEvents.size() == 0
        ? retrievedEvents
        : combine(retrievedEvents, createdEvents);
    return all.stream()
        .map(eventMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<CalendarEvent> saveAll(String idUser, String idCalendar, List<CalendarEvent> toSave) {
    Calendar calendar = calendarRepository.getById(idCalendar);
    List<Event> googleEvents = toSave.stream()
        .map(eventMapper::toEvent)
        .collect(Collectors.toList());
    return calendarApi.createEvents(idUser, calendar.getEteId(), googleEvents).stream()
        .map(eventMapper::toCalendarEvent)
        .collect(Collectors.toList());
  }

  private static List<HCalendarEvent> combine(List<HCalendarEvent> x1, List<HCalendarEvent> x2) {
    ArrayList<HCalendarEvent> all = new ArrayList<>(x1);
    all.addAll(x2);
    return all;
  }
}
