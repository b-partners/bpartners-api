package app.bpartners.api.repository;

import app.bpartners.api.model.CalendarEvent;
import java.time.Instant;
import java.util.List;

public interface CalendarEventRepository {
  List<CalendarEvent> findByIdUserAndIdCalendar(String idUser,
                                                String idCalendar,
                                                Instant from,
                                                Instant to);

  List<CalendarEvent> saveAll(String idUser, String idCalendar, List<CalendarEvent> toSave);

  List<CalendarEvent> removeAllByIdUser(String idUser);
}
