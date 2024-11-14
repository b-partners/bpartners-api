package app.bpartners.api.model.subscription;

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
    return subscriptions != null
        && !subscriptions.isEmpty()
        && subscriptions.stream()
            .sorted(comparing(Subscription::getCreationDatetime, naturalOrder()).reversed())
            .toList()
            .getFirst()
            .isActive();
  }
}
