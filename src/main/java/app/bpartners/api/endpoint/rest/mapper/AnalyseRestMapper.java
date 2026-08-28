package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateAnalyse;
import app.bpartners.api.model.prospect.Analyse;
import app.bpartners.api.repository.ProspectRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AnalyseRestMapper {
  private final ProspectRepository prospectRepository;
  private final ProspectRestMapper prospectRestMapper;

  public Analyse toDomain(String idProspect, CreateAnalyse createAnalyse) {
    return Analyse.builder()
        .prospect(prospectRepository.getById(idProspect))
        .metadata(createAnalyse.getMetadata())
        .build();
  }

  public app.bpartners.api.endpoint.rest.model.Analyse toRest(Analyse domain) {
    return new app.bpartners.api.endpoint.rest.model.Analyse()
        .id(domain.getId())
        .prospect(prospectRestMapper.toRest(domain.getProspect()))
        .metadata(domain.getMetadata())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt());
  }
}
