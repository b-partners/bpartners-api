package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Calendar;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.mapper.CalendarEventMapper;
import app.bpartners.api.repository.CalendarEventRepository;
import app.bpartners.api.repository.CalendarRepository;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.repository.google.calendar.CalendarApi.dateTimeFrom;

@Repository
@AllArgsConstructor
public class CalendarEventRepositoryImpl implements CalendarEventRepository {
  private final CalendarApi calendarApi;
  private final CalendarEventMapper eventMapper;
  private final CalendarRepository calendarRepository;


  @Override
  public List<CalendarEvent> findByIdUserAndIdCalendar(String idUser,
                                                       String idCalendar,
                                                       Instant from,
                                                       Instant to) {
    Calendar calendar = calendarRepository.getById(idCalendar);

    DateTime dateMin = dateTimeFrom(from);
    DateTime dateMax = dateTimeFrom(to);
    List<CalendarEvent> actualEvents =
        calendarApi.getEvents(idUser, calendar.getEteId(), dateMin, dateMax).stream()
            .map(eventMapper::toCalendarEvent)
            .collect(Collectors.toList());
    return actualEvents;
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
}
