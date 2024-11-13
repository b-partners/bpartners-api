package app.bpartners.api.model.subscription;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import app.bpartners.api.model.User;
import java.util.List;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscription {
  private User user;
  private List<Subscription> subscription;

  public boolean hasValidSubscription() {
    return subscription != null
        && !subscription.isEmpty()
        && subscription.stream()
            .sorted(comparing(Subscription::getCreationDatetime, naturalOrder()).reversed())
            .toList()
            .getFirst()
            .isActive();
  }
}
