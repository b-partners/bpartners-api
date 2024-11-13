package app.bpartners.api.model.subscription;

import java.time.Instant;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class Subscription {
  private String id;
  private boolean active;
  private SubscriptionType type;
  private SubscriptionPaymentMethod paymentMethod;
  private Instant validityDatetime;
  private Instant creationDatetime;
}
