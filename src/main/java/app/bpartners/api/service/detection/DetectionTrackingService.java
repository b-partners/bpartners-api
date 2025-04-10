package app.bpartners.api.service.detection;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.detection.DetectionTracking;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.repository.DetectionTrackingRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetectionTrackingService {
  private final DetectionTrackingRepository repository;
  private final SubscriptionService subscriptionService;
  private final CustomDateFormatter customDateFormatter;

  public List<DetectionTracking> findAllByIdUserBetween(String idUser, Instant from, Instant to) {
    return repository.findAllByIdUserBetween(idUser, from, to);
  }

  public List<DetectionTracking> saveAll(List<DetectionTracking> tracking) {
    return repository.saveAll(tracking);
  }

  @Transactional
  public List<DetectionTracking> computeTrackingWithSubscriptionConsumptionLog(
      List<DetectionTracking> tracking) {
    List<DetectionTracking> savedTracking = saveAll(tracking);
    savedTracking.forEach(
        saved ->
            subscriptionService.addConsumption(
                SubscriptionConsumptionLog.builder()
                    .id(randomUUID().toString())
                    .userId(saved.user().getId())
                    .consumptionType(ROOF_ANALYSIS)
                    .usageMetric(1L)
                    .consumptionUnit(UNIT)
                    .comment(getAnalysisComment(saved))
                    .creationDatetime(now())
                    .build()));
    return savedTracking;
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
