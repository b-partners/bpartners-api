package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.BusinessActivityTemplate;
import app.bpartners.api.model.mapper.BusinessActivityTemplateMapper;
import app.bpartners.api.repository.BusinessActivityTemplateRepository;
import app.bpartners.api.repository.jpa.BusinessActivityTemplateJpaRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BusinessActivityTemplateRepositoryImpl
    implements BusinessActivityTemplateRepository {
  private final BusinessActivityTemplateJpaRepository jpaRepository;
  private final BusinessActivityTemplateMapper domainMapper;

  @Override
  public List<BusinessActivityTemplate> findAll(Pageable pageable) {
    return jpaRepository.findAll(pageable)
        .map(domainMapper::toDomain).getContent();
  }
}
