package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HBusinessActivityTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessActivityTemplateJpaRepository
    extends JpaRepository<HBusinessActivityTemplate, String> {
  Optional<HBusinessActivityTemplate> findByNameIgnoreCase(String name);
}
