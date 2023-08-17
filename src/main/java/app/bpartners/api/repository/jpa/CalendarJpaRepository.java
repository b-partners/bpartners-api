package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HCalendar;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarJpaRepository extends JpaRepository<HCalendar, String> {
  List<HCalendar> findByIdUser(String idUser);
}
