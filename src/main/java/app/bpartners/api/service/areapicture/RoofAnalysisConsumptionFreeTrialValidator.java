package app.bpartners.api.service.areapicture;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static java.time.Instant.now;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.UserApiKeyFullAuthorizationJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.ZoneId;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoofAnalysisConsumptionFreeTrialValidator
    implements Consumer<UserSubscriptionEligible> {
  private static final long MAX_FREE_ROOF_ANALYSIS_CONSUMPTION_ALLOWED = 20L;
  private final SubscriptionService subscriptionService;
  private final UserApiKeyFullAuthorizationJpaRepository apiKeyFullAuthorizationRepository;

  @Override
  public void accept(UserSubscriptionEligible userSubscriptionEligible) {
    if (!userSubscriptionEligible.hasFreeTrialPeriodActive()) {
      return;
    }
    var userId = userSubscriptionEligible.getUserId();
    var trialPeriodStartDate = userSubscriptionEligible.getEligibleFrom();
    var trialPeriodStartInstant =
        trialPeriodStartDate.atStartOfDay().atZone(ZoneId.of("Europe/Paris")).toInstant();
    var now = now();

    var consumptionLogs =
        subscriptionService.findConsumptionLogsByUserId(userId, trialPeriodStartInstant, now);
    var actualRoofAnalysisConsumption =
        consumptionLogs.stream()
            .filter(log -> ROOF_ANALYSIS.equals(log.getConsumptionType()))
            .map(SubscriptionConsumptionLog::getUsageMetric)
            .reduce(Long::sum)
            .orElse(0L);

    if (actualRoofAnalysisConsumption >= MAX_FREE_ROOF_ANALYSIS_CONSUMPTION_ALLOWED
        && apiKeyFullAuthorizationRepository.findByIdUser(userId).isEmpty()) {
      throw new BadRequestException(
          "Roof analysis consumption "
              + actualRoofAnalysisConsumption
              + " limit exceeded for free trial period for User.id="
              + userId);
    }
  }
}
