package app.bpartners.api.model.subscription;

import java.time.Instant;
import java.util.List;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class Subscription {
  private String id;
  private String e2Id;
  private boolean active;
  private List<String> paymentMethods;
  private SubscriptionType type;
  private Instant validityDatetime;
  private Instant creationDatetime;
}
