package app.bpartners.api.model.mapper;

import app.bpartners.api.model.BusinessActivityTemplate;
import app.bpartners.api.repository.jpa.model.HBusinessActivityTemplate;
import org.springframework.stereotype.Component;

@Component
public class BusinessActivityTemplateMapper {
  public BusinessActivityTemplate toDomain(HBusinessActivityTemplate entity) {
    return BusinessActivityTemplate.builder()
        .id(entity.getId())
        .name(entity.getName())
        .build();
  }
}
