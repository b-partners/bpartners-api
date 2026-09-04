package app.bpartners.api.service.detection;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.detection.DetectionTracking;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.DetectionTrackingRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.subscription.RoofAnalysisConsumptionFreeTrialValidator;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  private final SubscriptionService subscriptionService;
  private final CustomDateFormatter customDateFormatter;
  private final UserSubscriptionEligibleJpaRepository userSubscriptionEligibleRepository;
  private final RoofAnalysisConsumptionFreeTrialValidator roofAnalysisConsumptionFreeTrialValidator;
  private final CreditService creditService;

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
  public List<DetectionTracking> computeTrackingWithSubscriptionConsumptionLog(
      List<DetectionTracking> tracking) {
    var unregisteredTracking = filterUnregistered(tracking);

    unregisteredTracking.stream()
        .map(DetectionTracking::user)
        .map(User::getId)
        .distinct()
        .forEach(this::validateRoofAnalysisFreeTrialConsumption);

    List<DetectionTracking> savedTracking = saveAll(unregisteredTracking);

    savedTracking.forEach(
        saved -> {
          var userId = saved.user().getId();
          var comment = getAnalysisComment(saved);
          subscriptionService.addConsumption(
              SubscriptionConsumptionLog.builder()
                  .id(randomUUID().toString())
                  .userId(userId)
                  .consumptionType(ROOF_ANALYSIS)
                  .usageMetric(1L)
                  .consumptionUnit(UNIT)
                  .comment(comment)
                  .creationDatetime(now())
                  .build());
          if (!hasFreeTrialPeriodActive(userId)) {
            creditService.consumeRoofAnalysis(userId, "Analyse toiture : " + saved.address());
          }
        });
    return savedTracking;
  }

  private List<DetectionTracking> filterUnregistered(List<DetectionTracking> tracking) {
    var unregistered = new ArrayList<DetectionTracking>();
    var identifiersOfBatch = new HashSet<String>();
    for (var detectionTracking : tracking) {
      var detectionIdentifier = detectionTracking.detectionIdentifier();
      if (detectionIdentifier == null
          || (identifiersOfBatch.add(detectionIdentifier)
              && repository.findByDetectionIdentifier(detectionIdentifier).isEmpty())) {
        unregistered.add(detectionTracking);
      }
    }
    return unregistered;
  }

  private void validateRoofAnalysisFreeTrialConsumption(String userId) {
    userSubscriptionEligibleRepository
        .findByUserId(userId)
        .ifPresent(roofAnalysisConsumptionFreeTrialValidator);
  }

  private boolean hasFreeTrialPeriodActive(String userId) {
    return userSubscriptionEligibleRepository
        .findByUserId(userId)
        .map(UserSubscriptionEligible::hasFreeTrialPeriodActive)
        .orElse(false);
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
