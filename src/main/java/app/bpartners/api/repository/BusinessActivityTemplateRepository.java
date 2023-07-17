package app.bpartners.api.repository;

import app.bpartners.api.model.BusinessActivityTemplate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface BusinessActivityTemplateRepository {
  List<BusinessActivityTemplate> findAll(Pageable pageable);

  List<BusinessActivityTemplate> findAll();
}
