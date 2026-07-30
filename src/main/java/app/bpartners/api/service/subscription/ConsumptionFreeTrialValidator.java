package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.WhiteListScope.API_KEY_NOT_RESTRICTED_BY_TRIAL;
import static java.time.Instant.now;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.SubscriptionConsumptionLogJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import java.time.ZoneId;
import java.util.function.Consumer;

public abstract class ConsumptionFreeTrialValidator implements Consumer<UserSubscriptionEligible> {
  protected static final long DEFAULT_MAX_CONSUMPTION_DURING_FREE_TRIAL = 20L;
  private static final ZoneId ZONE_ID_OF_EUROPE_PARIS = ZoneId.of("Europe/Paris");

  private final SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepository;
  private final UserWhiteListedJpaRepository userWhiteListedJpaRepository;

  protected ConsumptionFreeTrialValidator(
      SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepository,
      UserWhiteListedJpaRepository userWhiteListedJpaRepository) {
    this.consumptionLogJpaRepository = consumptionLogJpaRepository;
    this.userWhiteListedJpaRepository = userWhiteListedJpaRepository;
  }

  protected abstract SubscriptionConsumptionType consumptionType();

  protected abstract String consumptionLabel();

  protected long maxConsumptionDuringFreeTrial() {
    return DEFAULT_MAX_CONSUMPTION_DURING_FREE_TRIAL;
  }

  @Override
  public void accept(UserSubscriptionEligible userSubscriptionEligible) {
    var userId = userSubscriptionEligible.getUserId();

    var actualConsumption = consumptionSinceFreeTrialPeriodStart(userSubscriptionEligible);
    if (actualConsumption >= maxConsumptionDuringFreeTrial() && isRestrictedByFreeTrial(userId)) {
      throw new BadRequestException(
          consumptionLabel()
              + " consumption "
              + actualConsumption
              + " limit exceeded for free trial period for User.id="
              + userId);
    }
  }

  private long consumptionSinceFreeTrialPeriodStart(
      UserSubscriptionEligible userSubscriptionEligible) {
    var freeTrialPeriodStart =
        userSubscriptionEligible
            .getEligibleFrom()
            .atStartOfDay()
            .atZone(ZONE_ID_OF_EUROPE_PARIS)
            .toInstant();
    return consumptionLogJpaRepository
        .findAllByUserIdAndConsumptionTypeAndCreationDatetimeBetween(
            userSubscriptionEligible.getUserId(), consumptionType(), freeTrialPeriodStart, now())
        .stream()
        .map(SubscriptionConsumptionLog::getUsageMetric)
        .mapToLong(usageMetric -> usageMetric == null ? 0L : usageMetric)
        .sum();
  }

  private boolean isRestrictedByFreeTrial(String userId) {
    return userWhiteListedJpaRepository
        .findByUserId(userId)
        .filter(
            userWhiteListed ->
                userWhiteListed.getScopes().contains(API_KEY_NOT_RESTRICTED_BY_TRIAL))
        .isEmpty();
  }
}
