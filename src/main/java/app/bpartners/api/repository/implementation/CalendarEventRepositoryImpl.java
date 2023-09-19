package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Calendar;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.EventConnector;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.repository.google.calendar.CalendarApi.dateTimeFrom;

@Repository
@AllArgsConstructor
@Slf4j
//TODO: improve using connectors
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
    List<Event> eventEntries = getSyncGoogleEvents(idUser, calendar, dateMin, dateMax);
    List<HCalendarEvent> retrievedEvents =
        jpaRepository.findAllByIdUserAndIdCalendar(idUser, idCalendar);
    List<HCalendarEvent> eventEntities =
        eventEntries.stream()
            .map(event -> eventMapper.toEntity(randomUUID(), idUser, idCalendar, event))
            .collect(Collectors.toList());
    if (retrievedEvents.isEmpty()) {
      eventEntities.forEach(newEvent -> {
            newEvent.setId(randomUUID());
            newEvent.setSync(true);
            newEvent.setCreatedAt(Instant.now());
            newEvent.setNewEvent(true);
          }
      );
    } else {
      /*TODO: improve this
       *  The goal is to save only event with unknown end to end ID*/
      for (HCalendarEvent newEvent : eventEntities) {
        for (HCalendarEvent actualEvent : retrievedEvents) {
          if ((actualEvent.getEteId() != null && newEvent.getEteId() != null)
              && actualEvent.getEteId().equals(newEvent.getEteId())) {
            newEvent.setNewEvent(false);
            break;
          } else {
            newEvent.setId(randomUUID());
            newEvent.setSync(true);
            newEvent.setCreatedAt(Instant.now());
            newEvent.setNewEvent(true);
          }
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
    /*TODO: END*/

    return all.stream()
        .map(eventMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<CalendarEvent> saveAll(String idUser, String idCalendar, List<CalendarEvent> toSave) {
    Calendar calendar = calendarRepository.getById(idCalendar);

    List<EventConnector> googleEventsToSave = toSave.stream()
        .map(domain -> {
          String id = null;
          Optional<HCalendarEvent> optionalEvent = jpaRepository.findById(domain.getId());
          if (optionalEvent.isPresent()) {
            id = optionalEvent.get().getEteId();
          }
          return EventConnector.builder()
              .domainId(domain.getId())
              .googleEvent(eventMapper.toEvent(id, domain))
              .build();
        })
        .collect(Collectors.toList());

    List<EventConnector> savedEvents =
        getCrupdatedGoogleEvents(idUser, calendar, googleEventsToSave);

    List<HCalendarEvent> newEventEntities = savedEvents.isEmpty()
        ? new ArrayList<>()
        : savedEvents.stream()
        .map(connector -> eventMapper.toEntity(connector.getDomainId(), idUser, idCalendar,
            connector.getGoogleEvent()))
        .collect(Collectors.toList());
    if (newEventEntities.isEmpty()) {
      newEventEntities = toSave.stream()
          .map(event -> eventMapper.toEntity(idUser, idCalendar, event))
          .collect(Collectors.toList());
    } else {
      /*TODO: improve this
       * The goal is to update existing events by using existing ID*/
      List<HCalendarEvent> actualEventEntities =
          jpaRepository.findAllByIdUserAndIdCalendar(idUser, idCalendar);
      newEventEntities.forEach(newEvent -> {
        actualEventEntities.forEach(actualEvent -> {
          newEvent.setUpdatedAt(Instant.now());
          if ((actualEvent.getEteId() != null && newEvent.getEteId() != null)
              && actualEvent.getEteId().equals(newEvent.getEteId())) {
            newEvent.setId(actualEvent.getId());
          } else {
            newEvent.setId(String.valueOf(randomUUID()));
            newEvent.setSync(true);
            newEvent.setCreatedAt(Instant.now());
          }
        });
      });
    }
    /*TODO: END*/
    return jpaRepository.saveAll(newEventEntities).stream()
        .map(eventMapper::toDomain)
        .collect(Collectors.toList());
  }

  private List<Event> getSyncGoogleEvents(String idUser, Calendar calendar, DateTime dateMin,
                                          DateTime dateMax) {
    try {
      return calendarApi.getEvents(idUser, calendar.getEteId(), dateMin, dateMax);
    } catch (Exception e) {
      log.warn("Unable to synchronize with Google Calendar : " + e.getMessage());
      return List.of();
    }
  }

  //TODO: improve this
  private List<EventConnector> getCrupdatedGoogleEvents(String idUser,
                                                        Calendar calendar,
                                                        List<EventConnector> googleEvents) {
    try {
      googleEvents.forEach(event -> {
        var savedEvents = calendarApi.crupdateEvents(
            idUser,
            calendar.getEteId(),
            List.of(event.getGoogleEvent()));
        event.setGoogleEvent(savedEvents.get(0));
      });
      return googleEvents;
    } catch (Exception e) {
      log.warn("Unable to synchronize with Google Calendar : " + e.getMessage());
      return List.of();
    }
  }

  private static List<HCalendarEvent> combine(List<HCalendarEvent> x1, List<HCalendarEvent> x2) {
    ArrayList<HCalendarEvent> all = new ArrayList<>(x1);
    all.addAll(x2);
    return all;
  }

  private static String randomUUID() {
    return String.valueOf(UUID.randomUUID());
  }
}
