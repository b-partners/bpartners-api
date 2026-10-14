package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;

import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionUpgradeTimeline {
  public static final Duration PAYMENT_WINDOW = Duration.ofHours(48);
  public static final Duration PAYMENT_MARGIN = Duration.ofMinutes(60);
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

  private final TemporalUtils temporalUtils;

  public Instant billingCycleAnchorOf(UserSubscriptionUpgrade upgrade) {
    var upgradeDate = upgrade.getUpgradeDatetime().atZone(PARIS).toLocalDate();
    var firstPayment =
        upgrade.getToBillingInterval() == YEARLY
            ? upgradeDate.plusYears(1)
            : temporalUtils.startOfMonthAfter(upgradeDate);
    return firstPayment.atStartOfDay(PARIS).toInstant();
  }

  public Instant paymentExpirationOf(UserSubscriptionUpgrade upgrade) {
    var creation = upgrade.getCreationDatetime();
    var oldSubscriptionRenewal =
        upgrade
            .getOldCoverageEndDatetime()
            .atZone(PARIS)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(PARIS)
            .toInstant();
    var limit =
        Stream.of(
                creation.plus(PAYMENT_WINDOW),
                billingCycleAnchorOf(upgrade),
                oldSubscriptionRenewal)
            .min(Comparator.naturalOrder())
            .orElseThrow();
    var expiration = limit.minus(PAYMENT_MARGIN);
    return expiration.isBefore(creation) ? creation : expiration;
  }
}
