package app.bpartners.api.model.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;

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
  private SubscriptionStatus status;
  private SubscriptionProduct subscriptionProduct;

  @Builder.Default private BillingInterval billingInterval = MONTHLY;

  private Instant endDatetime;
  private Instant startDatetime;

  public enum SubscriptionStatus {
    ACTIVE,
    TRIALING,
    CANCELED,
    UNPAID,
    UNKNOWN
  }
}
