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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.repository.google.calendar.CalendarApi.dateTimeFrom;
import static app.bpartners.api.repository.google.calendar.CalendarApi.instantFrom;
import static java.util.UUID.randomUUID;

@Repository
@AllArgsConstructor
@Slf4j
public class GoogleCalendarEventRepository
    implements CalendarEventRepository {
  private final CalendarApi calendarApi;
  private final CalendarRepository calendarRepository;
  private final CalendarEventMapper eventMapper;
  private final CalendarEventJpaRepository jpaRepository;

  @Override
  public List<CalendarEvent> findByIdUserAndIdCalendar(String idUser, String idCalendar,
                                                       Instant from, Instant to) {
    Calendar calendar = calendarRepository.getById(idCalendar);
    DateTime dateMin = dateTimeFrom(from);
    DateTime dateMax = dateTimeFrom(to);
    List<Event> eventEntries = calendarApi.getEvents(idUser, calendar.getEteId(), dateMin, dateMax);
    List<CalendarEvent> googleConvertedEvents = toDomainFrom(eventEntries);
    List<CalendarEvent> newGoogleEvents = googleConvertedEvents.stream()
        .filter(CalendarEvent::isNewEvent)
        .collect(Collectors.toList());
    if (!newGoogleEvents.isEmpty()) {
      jpaRepository.saveAll(newGoogleEvents.stream()
          .map(event -> eventMapper.toEntity(idUser, idCalendar, event))
          .collect(Collectors.toList()));
    }
    return googleConvertedEvents;
  }

  @Override
  public List<CalendarEvent> saveAll(String idUser, String idCalendar, List<CalendarEvent> toSave) {
    Calendar calendar = calendarRepository.getById(idCalendar);
    List<Event> googleEventsToSave = toSave.stream()
        .map(event -> {
          String id = null;
          Optional<HCalendarEvent> optionalEvent = jpaRepository.findById(event.getId());
          if (optionalEvent.isPresent()) {
            id = optionalEvent.get().getEteId();
          }
          return eventMapper.toEvent(id, event);
        })
        .collect(Collectors.toList());
    List<Event> savedGoogleEvents =
        calendarApi.crupdateEvents(idUser, calendar.getEteId(), googleEventsToSave);
    return toDomainFrom(savedGoogleEvents);
  }

  private List<CalendarEvent> toDomainFrom(List<Event> crupdatedEvents) {
    return crupdatedEvents.stream()
        .map(googleEvent -> {
          String id = String.valueOf(randomUUID());
          boolean isSync = true;
          boolean isNewEvent = true;
          Instant actualUpdatedAt = instantFrom(googleEvent.getUpdated());
          /*TODO: check why end_to_end_id is persisted twice*/
          List<HCalendarEvent> optionalEvent = jpaRepository.findAllByEteId(googleEvent.getId());
          if (!optionalEvent.isEmpty()) {
            List<HCalendarEvent> sortedByUpdatedAtDesc = optionalEvent.stream()
                .sorted(Comparator.comparing(HCalendarEvent::getUpdatedAt).reversed())
                .collect(Collectors.toList());
            HCalendarEvent eventEntity = optionalEvent.get(0);
            //Remove this delete when end_to_end_id is NOT persisted twice anymore
            if (sortedByUpdatedAtDesc.remove(eventEntity)) {
              jpaRepository.deleteAllById(sortedByUpdatedAtDesc.stream()
                  .map(HCalendarEvent::getId)
                  .collect(Collectors.toList())
              );
            }
            /*TODO: END*/
            Instant savedUpdatedAt = eventEntity.getUpdatedAt();
            id = eventEntity.getId();
            isNewEvent = false;
            isSync = actualUpdatedAt.compareTo(savedUpdatedAt) == 0;
          }
          return eventMapper.toDomain(id, isSync, isNewEvent, actualUpdatedAt, googleEvent);
        })
        .collect(Collectors.toList());
  }
}
