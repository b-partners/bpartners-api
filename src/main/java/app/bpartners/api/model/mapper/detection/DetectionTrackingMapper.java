package app.bpartners.api.model.mapper.detection;

import app.bpartners.api.model.User;
import app.bpartners.api.model.detection.DetectionInitiator;
import app.bpartners.api.model.detection.DetectionTracking;
import app.bpartners.api.repository.jpa.model.detection.HDetectionTracking;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class DetectionTrackingMapper {

  public @NotNull HDetectionTracking toEntity(DetectionTracking domain) {
    return HDetectionTracking.builder()
        .id(domain.id())
        .zone(domain.zone())
        .address(domain.address())
        .initiatorName(domain.detectionInitiator().name())
        .initiatorEmail(domain.detectionInitiator().email())
        .initiatorPhoneNumber(domain.detectionInitiator().phoneNumber())
        .creationDatetime(domain.creationDatetime())
        .idUser(domain.user().getId())
        .detectionIdentifier(domain.detectionIdentifier())
        .projectKey(domain.projectKey())
        .externalReference(domain.externalReference())
        .build();
  }

  public @NotNull DetectionTracking toDomain(User user, HDetectionTracking entity) {
    return new DetectionTracking(
        entity.getId(),
        entity.getZone(),
        entity.getAddress(),
        entity.getCreationDatetime(),
        toDomainInitiator(entity),
        user,
        entity.getDetectionIdentifier(),
        entity.getProjectKey(),
        entity.getExternalReference());
  }

  public @NotNull DetectionInitiator toDomainInitiator(HDetectionTracking entity) {
    return new DetectionInitiator(
        entity.getInitiatorName(), entity.getInitiatorEmail(), entity.getInitiatorPhoneNumber());
  }
}
