package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Calendar;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.EventConnector;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.CalendarEventMapper;
import app.bpartners.api.repository.CalendarEventRepository;
import app.bpartners.api.repository.CalendarRepository;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.jpa.CalendarEventJpaRepository;
import app.bpartners.api.repository.jpa.model.HCalendarEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class FacadeCalendarEventRepository
    implements CalendarEventRepository {
  private final CalendarApi calendarApi;
  private final CalendarEventMapper eventMapper;
  private final CalendarRepository calendarRepository;
  private final CalendarEventJpaRepository jpaRepository;

  @Override
  public List<CalendarEvent> findByIdUserAndIdCalendar(String idUser,
                                                       String idCalendar,
                                                       Instant from,
                                                       Instant to) {
    throw new NotImplementedException("Not supported");
  }

  //TODO: improve using correctly facade and connectors design pattern, ie, use Google and Local Repository
  @Override
  public List<CalendarEvent> saveAll(String idUser, String idCalendar, List<CalendarEvent> toSave) {
    Calendar calendar = calendarRepository.getById(idCalendar);

    List<EventConnector> googleEventsToSave = toSave.stream()
        .map(domain -> {
          String eteId = null;
          Optional<HCalendarEvent> optionalEvent = jpaRepository.findById(domain.getId());
          if (optionalEvent.isPresent()) {
            eteId = optionalEvent.get().getEteId();
          }
          return EventConnector.builder()
              .domainId(domain.getId())
              .googleEvent(eventMapper.toEvent(eteId, domain))
              .build();
        })
        .collect(Collectors.toList());

    List<EventConnector> savedGoogleEventConnectors =
        getSavedGoogleEvents(idUser, calendar, googleEventsToSave);

    List<HCalendarEvent> eventEntities = savedGoogleEventConnectors.isEmpty()
        ? toSave.stream()
        .map(event -> eventMapper.toEntity(idUser, idCalendar, event))
        .collect(Collectors.toList())
        : savedGoogleEventConnectors.stream()
        .map(connector -> eventMapper.toEntity(
            connector.getDomainId(),
            idUser,
            idCalendar,
            connector.getGoogleEvent()))
        .collect(Collectors.toList());
    if (!savedGoogleEventConnectors.isEmpty()) {
      List<HCalendarEvent> actualEventEntities =
          jpaRepository.findAllByIdUserAndIdCalendar(idUser, idCalendar);
      eventEntities.forEach(newEvent -> {
        newEvent.setSync(true);
        actualEventEntities.forEach(actualEvent -> {
          if ((actualEvent.getEteId() != null && newEvent.getEteId() != null)
              && actualEvent.getEteId().equals(newEvent.getEteId())) {
            newEvent.setId(actualEvent.getId());
          } else {
            newEvent.setCreatedAt(Instant.now());
          }
        });
      });
    }
    List<HCalendarEvent> savedEntities = jpaRepository.saveAll(eventEntities);
    //TODO: improve this
    for (var savedEntity : savedEntities) {
      for (var newEvent : eventEntities) {
        if (savedEntity.getId().equals(newEvent.getId())) {
          savedEntity.setSync(newEvent.isSync());
          break;
        }
      }
    }
    return savedEntities.stream()
        .map(eventMapper::toDomain)
        .collect(Collectors.toList());
  }

  //TODO: improve this
  private List<EventConnector> getSavedGoogleEvents(String idUser,
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
}
