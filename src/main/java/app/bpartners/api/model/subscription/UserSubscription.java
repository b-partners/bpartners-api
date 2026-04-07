package app.bpartners.api.model.subscription;

import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.CANCELED;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.UNPAID;
import static java.time.LocalTime.now;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import app.bpartners.api.model.User;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscription {
  private User user;
  private List<Subscription> subscriptions;

  public boolean hasValidSubscription() {
    return getLatestSubscription() != null
        && getLatestSubscription().isActive()
        && !getLatestSubscription().getEndDatetime().isBefore(Instant.now());
  }

  public boolean hasLateSubscriptionPayment() {
    return getSubscriptions().stream()
        .anyMatch(subscription -> UNPAID.equals(subscription.getStatus()));
  }

  public boolean hasSubscriptionCancelled() {
    if (getLatestSubscription() == null || getLatestSubscription().getEndDatetime() == null)
      return false;
    var latestSubscriptionYearMonth = YearMonth.from(getLatestSubscription().getEndDatetime());
    var subscriptionYear = latestSubscriptionYearMonth.getYear();
    var subscriptionMonth = latestSubscriptionYearMonth.getMonth().getValue();
    var actualYearMonth = YearMonth.from(now());
    var actualYear = actualYearMonth.getYear();
    var actualMonth = actualYearMonth.getMonth().getValue();
    var subscriptionEndsActualOrNextMonthThisYear =
        actualMonth != 12
            && subscriptionYear == actualYear
            && (subscriptionMonth == actualMonth || subscriptionMonth == actualMonth + 1);
    var subscriptionEndsNextMonthNextYear =
        actualMonth == 12 && subscriptionYear == actualYear + 1 && subscriptionMonth == 1;
    return (subscriptionEndsActualOrNextMonthThisYear || subscriptionEndsNextMonthNextYear)
        && (CANCELED).equals(getLatestSubscription().getStatus());
  }

  public Subscription getLatestSubscription() {
    if (subscriptions.isEmpty()) {
      return null;
    }
    return subscriptions.stream()
        .sorted(comparing(Subscription::getStartDatetime, naturalOrder()).reversed())
        .toList()
        .getFirst();
  }
}
