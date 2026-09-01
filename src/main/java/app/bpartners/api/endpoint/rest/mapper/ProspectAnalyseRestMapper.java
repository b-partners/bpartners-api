package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateProspectAnalyse;
import app.bpartners.api.model.prospect.ProspectAnalyse;
import app.bpartners.api.repository.ProspectRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProspectAnalyseRestMapper {
  private final ProspectRepository prospectRepository;
  private final ProspectRestMapper prospectRestMapper;

  public ProspectAnalyse toDomain(String idProspect, CreateProspectAnalyse createProspectAnalyse) {
    return ProspectAnalyse.builder()
        .prospect(prospectRepository.getById(idProspect))
        .metadata(createProspectAnalyse.getMetadata())
        .build();
  }

  public app.bpartners.api.endpoint.rest.model.ProspectAnalyse toRest(ProspectAnalyse domain) {
    return new app.bpartners.api.endpoint.rest.model.ProspectAnalyse()
        .id(domain.getId())
        .prospect(prospectRestMapper.toRest(domain.getProspect()))
        .metadata(domain.getMetadata())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt());
  }
}
