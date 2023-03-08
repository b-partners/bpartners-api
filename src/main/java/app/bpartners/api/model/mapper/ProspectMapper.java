package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProspectMapper {
  private final AuthenticatedResourceProvider provider;
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

  public Prospect toDomain(HProspect entity) {
    return Prospect.builder()
        .id(entity.getId())
        .email(entity.getEmail())
        .address(entity.getAddress())
        .name(entity.getName())
        .phone(entity.getPhone())
        .status(entity.getStatus())
        .build();
  }
}
