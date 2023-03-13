package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ProspectMapper {

  private final AuthenticatedResourceProvider provider;
  private final SogefiBuildingPermitRepository sogefiRepository;
  private final ProspectJpaRepository jpaRepository;

  public HProspect toEntity(Prospect domain) {
    if (!jpaRepository.existsById(domain.getId())) {
      throw new NotFoundException("Prospect." + domain.getId() + " not found. ");
    }
    return HProspect.builder()
        .id(domain.getId())
        .phone(domain.getPhone())
        .name(domain.getName())
        .email(domain.getEmail())
        .status(domain.getStatus())
        .address(domain.getAddress())
        .idAccountHolder(provider.getAccountHolder().getId())
        .build();
  }

  public Prospect toDomain(HProspect entity, boolean isSogefiProspector) {
    Geojson location = null;
    if (isSogefiProspector) {
      location = sogefiRepository.findLocationByIdProspect(entity.getId());
      if (location == null) {
        log.warn("Prospect." + entity.getId() + " not found in prospecting database.");
      }
    }
    return Prospect.builder()
        .id(entity.getId())
        .email(entity.getEmail())
        .address(entity.getAddress())
        .name(entity.getName())
        .phone(entity.getPhone())
        .location(location)
        .status(entity.getStatus())
        .build();
  }
}
