package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.ProspectHistory;
import app.bpartners.api.repository.jpa.model.HProspectHistory;
import java.time.Instant;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class ProspectHistoryMapper {
  public HProspectHistory toEntity(ProspectHistory domain) {
    return HProspectHistory.builder()
        .id(domain.getId())
        .idAccountHolder(domain.getIdAccountHolder())
        .idProspect(domain.getIdProspect())
        .updatedAt(domain.getUpdatedAt() == null ? Instant.now() : domain.getUpdatedAt())
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

  public ProspectHistory toDomain(String prospectId, ProspectStatus status,
                                  String accountHolderId) {
    return ProspectHistory.builder()
        .idProspect(prospectId)
        .idAccountHolder(accountHolderId)
        .status(status)
        .updatedAt(Instant.now())
        .build();
  }
}
