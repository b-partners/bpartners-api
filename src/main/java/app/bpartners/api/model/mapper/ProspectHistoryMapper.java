package app.bpartners.api.model.mapper;

import app.bpartners.api.model.ProspectHistory;
import app.bpartners.api.repository.jpa.model.HProspectHistory;
import org.springframework.stereotype.Component;

@Component
public class ProspectHistoryMapper {
  public HProspectHistory toEntity(ProspectHistory domain) {
    return HProspectHistory.builder()
        .id(domain.getId())
        .idAccountHolder(domain.getIdAccountHolder())
        .idProspect(domain.getIdProspect())
        .updatedAt(domain.getUpdatedAt())
        .status(domain.getStatus())
        .build();
  }

  public ProspectHistory toDomain(HProspectHistory entity) {
    return ProspectHistory.builder()
        .id(entity.getId())
        .idProspect(entity.getIdProspect())
        .idAccountHolder(entity.getIdAccountHolder())
        .status(entity.getStatus())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
