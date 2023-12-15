package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HEmail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailJpaRepository extends JpaRepository<HEmail, String> {
  List<HEmail> findAllByIdUser(String idUser);
}
