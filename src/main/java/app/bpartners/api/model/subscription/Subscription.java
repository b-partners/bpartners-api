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
  private SubscriptionProduct subscriptionProduct;
  private Long freeTrialDays;
  private Instant freeTrialStart;
  private Instant freeTrialEnd;
  private Instant endDatetime;
  private Instant startDatetime;

  public boolean hasFreeTrialPeriod() {
    return freeTrialDays > 0;
  }
}
