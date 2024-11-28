package app.bpartners.api.model.subscription;

import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.CANCELLED;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import app.bpartners.api.model.User;
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
    return getLatestSubscription() != null && getLatestSubscription().isActive();
  }

  public boolean hasSubscriptionCancelled() {
    return getLatestSubscription() != null
        && (CANCELLED).equals(getLatestSubscription().getStatus());
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
