package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.repository.jpa.model.HProspect;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProspectMapper {
  private final AuthenticatedResourceProvider provider;

  public HProspect toEntity(Prospect domain) {
    return HProspect.builder()
        .phone(domain.getPhone())
        .name(domain.getName())
        .email(domain.getEmail())
        .status(domain.getStatus())
        .location(domain.getLocation())
        .idAccountHolder(provider.getAccountHolder().getId())
        .build();
  }

  public Prospect toDomain(HProspect entity) {
    return Prospect.builder()
        .id(entity.getId())
        .email(entity.getEmail())
        .location(entity.getLocation())
        .name(entity.getName())
        .phone(entity.getPhone())
        .status(entity.getStatus())
        .build();
  }
}
