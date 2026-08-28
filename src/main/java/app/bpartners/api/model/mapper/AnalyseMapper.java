package app.bpartners.api.model.mapper;

import app.bpartners.api.model.prospect.Analyse;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.jpa.model.HAnalyse;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AnalyseMapper {
  private final ProspectRepository prospectRepository;

  public HAnalyse toEntity(Analyse domain) {
    return HAnalyse.builder()
        .id(domain.getId())
        .idProspect(Objects.requireNonNull(domain.getProspect()).getId())
        .metadata(domain.getMetadata())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }

  public Analyse toDomain(HAnalyse entity) {
    return Analyse.builder()
        .id(entity.getId())
        .prospect(prospectRepository.getById(entity.getIdProspect()))
        .metadata(entity.getMetadata())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
