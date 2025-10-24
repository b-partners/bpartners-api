package app.bpartners.api.service.areapicture;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static java.time.Instant.now;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.UserApiKeyFullAuthorizationJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoofAnalysisConsumptionFreeTrialValidator
    implements Consumer<UserSubscriptionEligible> {
  private static final long DEFAULT_MAX_CONSUMPTION = 20L;
  private final SubscriptionService subscriptionService;
  private final UserApiKeyFullAuthorizationJpaRepository apiKeyFullAuthorizationRepository;
  private static final List<String> EXCLUDED_USER_IDS = List.of(
          "6d394379-585e-4471-b42e-213dc7624a55",
          "2ede5d19-fa49-4ad7-aa90-42c016a3f4f5"
  );

  @Override
  public void accept(UserSubscriptionEligible userSubscriptionEligible) {
    var userId = userSubscriptionEligible.getUserId();

    if (!userSubscriptionEligible.hasFreeTrialPeriodActive() || EXCLUDED_USER_IDS.contains(userId)) {
      return;
    }

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

    if (actualRoofAnalysisConsumption >= DEFAULT_MAX_CONSUMPTION
        &&  apiKeyFullAuthorizationRepository.findByIdUser(userId).isEmpty()) {
      throw new BadRequestException(
          "Roof analysis consumption "
              + actualRoofAnalysisConsumption
              + " limit exceeded for free trial period for User.id="
              + userId);
    }
  }
}
