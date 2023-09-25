package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Calendar;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.mapper.CalendarEventMapper;
import app.bpartners.api.repository.CalendarEventRepository;
import app.bpartners.api.repository.CalendarRepository;
import app.bpartners.api.repository.jpa.CalendarEventJpaRepository;
import app.bpartners.api.repository.jpa.model.HCalendarEvent;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class LocalCalendarEventRepository
    implements CalendarEventRepository {
  private final CalendarEventMapper eventMapper;
  private final CalendarRepository calendarRepository;
  private final CalendarEventJpaRepository jpaRepository;

  @Override
  public List<CalendarEvent> findByIdUserAndIdCalendar(String idUser, String idCalendar,
                                                       Instant from, Instant to) {
    Calendar calendar = calendarRepository.getById(idCalendar);
    return jpaRepository.findAllByIdUserAndIdCalendar(idUser, calendar.getId()).stream()
        .map(eventMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<CalendarEvent> saveAll(String idUser, String idCalendar, List<CalendarEvent> toSave) {
    Calendar calendar = calendarRepository.getById(idCalendar);
    List<HCalendarEvent> entities = toSave.stream()
        .map(event -> eventMapper.toEntity(idUser, calendar.getId(), event))
        .collect(Collectors.toList());
    return jpaRepository.saveAll(entities).stream()
        .map(eventMapper::toDomain)
        .collect(Collectors.toList());
  }
}
