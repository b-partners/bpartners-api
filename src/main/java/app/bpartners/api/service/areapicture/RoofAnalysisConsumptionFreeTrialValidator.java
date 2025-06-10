package app.bpartners.api.service.areapicture;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static java.time.Instant.now;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.UserService;
import java.time.ZoneId;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoofAnalysisConsumptionFreeTrialValidator
    implements Consumer<UserSubscriptionEligible> {
  private static final long MAX_FREE_ROOF_ANALYSIS_CONSUMPTION_ALLOWED = 10L;
  private final SubscriptionService subscriptionService;
  private final UserService userService;

  @Override
  public void accept(UserSubscriptionEligible userSubscriptionEligible) {
    if (!userSubscriptionEligible.hasFreeTrialPeriodActive()) {
      return;
    }
    var userId = userSubscriptionEligible.getUserId();
    var user = userService.getUserById(userId);
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
        && user.getApiKey() == null) {
      throw new BadRequestException(
          "Roof analysis consumption "
              + actualRoofAnalysisConsumption
              + " limit exceeded for free trial period for User.id="
              + userId);
    }
  }
}
