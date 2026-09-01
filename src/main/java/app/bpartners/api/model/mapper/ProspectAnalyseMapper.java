package app.bpartners.api.model.mapper;

import app.bpartners.api.model.prospect.ProspectAnalyse;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.jpa.model.HProspectAnalyse;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProspectAnalyseMapper {
  private final ProspectRepository prospectRepository;

  public HProspectAnalyse toEntity(ProspectAnalyse domain) {
    return HProspectAnalyse.builder()
        .id(domain.getId())
        .idProspect(Objects.requireNonNull(domain.getProspect()).getId())
        .metadata(domain.getMetadata())
        .posLongitude(domain.getLongitude())
        .posLatitude(domain.getLatitude())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }

  public ProspectAnalyse toDomain(HProspectAnalyse entity) {
    return ProspectAnalyse.builder()
        .id(entity.getId())
        .prospect(prospectRepository.getById(entity.getIdProspect()))
        .metadata(entity.getMetadata())
        .longitude(entity.getPosLongitude())
        .latitude(entity.getPosLatitude())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
