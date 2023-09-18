package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HCalendarEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarEventJpaRepository extends JpaRepository<HCalendarEvent, String> {
  List<HCalendarEvent> findByIdUser(String idUser);

  Optional<HCalendarEvent> findByEteId(String eteId);
}
