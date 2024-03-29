package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Calendar;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.CalendarMapper;
import app.bpartners.api.repository.CalendarRepository;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.jpa.CalendarJpaRepository;
import app.bpartners.api.repository.jpa.model.HCalendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
// TODO: improve using connectors
public class CalendarRepositoryImpl implements CalendarRepository {
  private final CalendarApi calendarApi;
  private final CalendarJpaRepository jpaRepository;
  private final CalendarMapper calendarMapper;

  @Override
  public List<Calendar> findByIdUser(String idUser) {
    List<CalendarListEntry> calendarEntries;
    try {
      calendarEntries = calendarApi.getCalendars(idUser);
    } catch (Exception e) {
      log.warn("Unable to synchronize with Google Calendar : " + e.getMessage());
      calendarEntries = List.of();
    }
    List<HCalendar> retrievedCalendars = jpaRepository.findByIdUser(idUser);

    List<HCalendar> calendarEntities =
        calendarEntries.stream()
            .map(entry -> calendarMapper.toCalendarEntity(idUser, entry))
            .toList();
    for (HCalendar newEntity : calendarEntities) {
      newEntity.setNewCalendar(true);
      newEntity.setCreatedAt(Instant.now());
      for (HCalendar actualEntity : retrievedCalendars) {
        if (actualEntity.getEteId().equals(newEntity.getEteId())) {
          newEntity.setNewCalendar(false);
          break;
        }
      }
    }
    List<HCalendar> createdCalendars =
        calendarEntities.isEmpty()
            ? List.of()
            : jpaRepository.saveAll(
                calendarEntities.stream()
                    .filter(HCalendar::isNewCalendar)
                    .collect(Collectors.toList()));
    List<HCalendar> all =
        createdCalendars.size() == 0
            ? retrievedCalendars
            : combine(retrievedCalendars, createdCalendars);

    return all.stream().map(calendarMapper::toCalendar).collect(Collectors.toList());
  }

  @Override
  public List<Calendar> removeAllByIdUser(String idUser) {
    List<Calendar> userCalendars = findByIdUser(idUser);
    jpaRepository.deleteAllById(userCalendars.stream().map(Calendar::getId).toList());
    return userCalendars;
  }

  @Override
  public Optional<Calendar> findById(String id) {
    return jpaRepository.findById(id).map(calendarMapper::toCalendar);
  }

  @Override
  public Calendar getById(String id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("Calendar(id=" + id + ") is not found"));
  }

  private static List<HCalendar> combine(List<HCalendar> x1, List<HCalendar> x2) {
    ArrayList<HCalendar> all = new ArrayList<>(x1);
    all.addAll(x2);
    return all;
  }
}
