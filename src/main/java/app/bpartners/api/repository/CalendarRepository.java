package app.bpartners.api.repository;

import app.bpartners.api.model.Calendar;
import java.util.List;
import java.util.Optional;

public interface CalendarRepository {
  List<Calendar> findByIdUser(String idUser);

  Optional<Calendar> findById(String id);

  Calendar getById(String id);
}
