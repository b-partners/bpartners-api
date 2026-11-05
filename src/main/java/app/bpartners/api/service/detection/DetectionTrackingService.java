package app.bpartners.api.service.detection;

import static app.bpartners.api.model.activity.UserActivityType.ROOF_DETECTION_2D;
import static app.bpartners.api.model.activity.UserActivityUnit.UNIT;
import static app.bpartners.api.model.project.AnalysisProjectStep.ANALYSIS_2D;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.activity.UserActivity;
import app.bpartners.api.model.detection.DetectionTracking;
import app.bpartners.api.model.project.AnalysisProjectDescription;
import app.bpartners.api.repository.DetectionTrackingRepository;
import app.bpartners.api.service.activity.UserActivityService;
import app.bpartners.api.service.project.AnalysisProjectService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetectionTrackingService {
  private static final int DEFAULT_PAGE_SIZE = 100;
  private final DetectionTrackingRepository repository;
  private final UserActivityService userActivityService;
  private final CustomDateFormatter customDateFormatter;
  private final AnalysisProjectService analysisProjectService;

  public List<DetectionTracking> findAllByIdUserBetween(String idUser, Instant from, Instant to) {
    return repository.findAllByIdUserBetween(idUser, from, to);
  }

  public List<DetectionTracking> findAllByIdUser(
      String idUser, String search, PageFromOne page, BoundedPageSize pageSize) {
    var pageValue = page != null ? page.getValue() - 1 : 0;
    var pageSizeValue = pageSize != null ? pageSize.getValue() : DEFAULT_PAGE_SIZE;
    return repository.findAllByIdUser(idUser, search, PageRequest.of(pageValue, pageSizeValue));
  }

  public List<DetectionTracking> saveAll(List<DetectionTracking> tracking) {
    return repository.saveAll(tracking);
  }

  @Transactional
  public List<DetectionTracking> computeTrackingWithUserActivity(List<DetectionTracking> tracking) {
    var unregisteredTracking = filterUnregistered(tracking);

    List<DetectionTracking> savedTracking = saveAll(unregisteredTracking);

    savedTracking.forEach(
        saved -> {
          var userId = saved.user().getId();
          var comment = getAnalysisComment(saved);
          userActivityService.record(
              UserActivity.builder()
                  .id(randomUUID().toString())
                  .userId(userId)
                  .activityType(ROOF_DETECTION_2D)
                  .quantity(1L)
                  .activityUnit(UNIT)
                  .description(comment)
                  .creationDatetime(now())
                  .build());
          analysisProjectService.register(
              userId,
              projectKeyOf(saved),
              saved.externalReference(),
              ANALYSIS_2D,
              "Analyse toiture : " + saved.zone(),
              descriptionOf(saved));
        });
    return savedTracking;
  }

  private AnalysisProjectDescription descriptionOf(DetectionTracking tracking) {
    var initiator = tracking.detectionInitiator();
    return new AnalysisProjectDescription(
        tracking.detectionIdentifier(),
        tracking.zone(),
        tracking.address(),
        initiator.name(),
        initiator.email(),
        initiator.phoneNumber());
  }

  private String projectKeyOf(DetectionTracking tracking) {
    var projectKey = tracking.projectKey();
    var hasExternalReference =
        tracking.externalReference() != null && !tracking.externalReference().isBlank();
    if ((projectKey == null || projectKey.isBlank()) && !hasExternalReference) {
      return "tracking:" + tracking.id();
    }
    return projectKey;
  }

  private List<DetectionTracking> filterUnregistered(List<DetectionTracking> tracking) {
    var unregistered = new ArrayList<DetectionTracking>();
    var identifiersOfBatch = new HashSet<String>();
    for (var detectionTracking : tracking) {
      var identifierRegistered =
          isIdentifierRegistered(detectionTracking.detectionIdentifier(), identifiersOfBatch);
      var zoneRegistered =
          isZoneRegistered(detectionTracking.user().getId(), detectionTracking.zone());
      if (!identifierRegistered && !zoneRegistered) {
        unregistered.add(detectionTracking);
      }
    }
    return unregistered;
  }

  private boolean isIdentifierRegistered(
      String detectionIdentifier, Set<String> identifiersOfBatch) {
    if (detectionIdentifier == null) {
      return false;
    }
    if (!identifiersOfBatch.add(detectionIdentifier)) {
      return true;
    }
    return repository.findByDetectionIdentifier(detectionIdentifier).isPresent();
  }

  private boolean isZoneRegistered(String idUser, String zone) {
    return zone != null && repository.existsByIdUserAndZoneIgnoreCase(idUser, zone);
  }

  private @NotNull String getAnalysisComment(DetectionTracking saved) {
    return "Analyse de toiture effectuée par le client"
        + " "
        + saved.detectionInitiator().name()
        + " (email="
        + saved.detectionInitiator().email()
        + ","
        + " tel="
        + saved.detectionInitiator().phoneNumber()
        + ")"
        + " sur la zone "
        + saved.zone()
        + " à l'adresse "
        + saved.address()
        + " le "
        + customDateFormatter.formatFrenchDatetime(saved.creationDatetime());
  }
}
