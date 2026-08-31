package app.bpartners.api.endpoint.rest.mapper.detection;

import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.CreateDetectionTracking;
import app.bpartners.api.endpoint.rest.model.DetectionTracking;
import app.bpartners.api.model.User;
import app.bpartners.api.model.detection.DetectionInitiator;
import app.bpartners.api.model.exception.BadRequestException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class DetectionTrackingRestMapper {

  public DetectionTracking toRest(app.bpartners.api.model.detection.DetectionTracking tracking) {
    return new DetectionTracking()
        .id(tracking.id())
        .detectionIdentifier(tracking.detectionIdentifier())
        .zone(tracking.zone())
        .address(tracking.address())
        .creationDatetime(tracking.creationDatetime())
        .initiator(
            new app.bpartners.api.endpoint.rest.model.DetectionInitiator()
                .name(tracking.detectionInitiator().name())
                .email(tracking.detectionInitiator().email())
                .phoneNumber(tracking.detectionInitiator().phoneNumber()));
  }

  public app.bpartners.api.model.detection.@NotNull DetectionTracking toDomain(
      CreateDetectionTracking createDetectionTracking, User user) {
    // TODO: set once geo-jobs updated
    // accept(createDetectionTracking);
    return new app.bpartners.api.model.detection.DetectionTracking(
        randomUUID().toString(),
        createDetectionTracking.getZone(),
        createDetectionTracking.getAddress(),
        createDetectionTracking.getCreationDatetime(),
        new DetectionInitiator(
            createDetectionTracking.getInitiator().getName(),
            createDetectionTracking.getInitiator().getEmail(),
            createDetectionTracking.getInitiator().getPhoneNumber()),
        user,
        createDetectionTracking.getDetectionIdentifier());
  }

  private void accept(CreateDetectionTracking createDetectionTracking) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createDetectionTracking.getDetectionIdentifier() == null) {
      exceptionMessageBuilder.append("Detection identifier is mandatory.");
    }
    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
