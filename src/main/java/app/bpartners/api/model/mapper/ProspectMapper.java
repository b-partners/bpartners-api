package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import java.time.Instant;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ProspectMapper {

  private final AuthenticatedResourceProvider provider;
  private final ProspectJpaRepository jpaRepository;

  public HProspect toEntity(Prospect domain) {
    Optional<HProspect> optionalProspect = jpaRepository.findById(domain.getId());
    if (optionalProspect.isEmpty()) {
      throw new NotFoundException("Prospect." + domain.getId() + " not found. ");
    }
    HProspect existing = optionalProspect.get();
    return toEntity(domain,
        provider.getDefaultAccountHolder().getId(),
        existing.getRating(),
        existing.getLastEvaluationDate());
  }

  public HProspect toEntity(Prospect domain, String prospectOwnerId, Double rating,
                            Instant lastEvaluationDate) {
    Geojson location = domain.getLocation();
    return HProspect.builder()
        .id(domain.getId())
        .phone(domain.getPhone())
        .name(domain.getName())
        .email(domain.getEmail())
        .status(domain.getStatus())
        .address(domain.getAddress())
        .idAccountHolder(prospectOwnerId)
        .townCode(domain.getTownCode())
        .rating(rating)
        .lastEvaluationDate(lastEvaluationDate)
        .posLongitude(location == null ? null : location.getLongitude())
        .posLatitude(location == null ? null : location.getLatitude())
        .build();
  }

  public Prospect toDomain(HProspect entity, Geojson location) {
    return Prospect.builder()
        .id(entity.getId())
        .idHolderOwner(entity.getIdAccountHolder())
        .email(entity.getEmail())
        .address(entity.getAddress())
        .name(entity.getName())
        .phone(entity.getPhone())
        .location(location)
        .status(entity.getStatus())
        .townCode(entity.getTownCode())
        .rating(Prospect.ProspectRating.builder()
            .value(entity.getRating())
            .lastEvaluationDate(entity.getLastEvaluationDate())
            .build())
        .build();
  }
}
