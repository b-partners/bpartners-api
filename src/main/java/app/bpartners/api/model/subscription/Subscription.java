package app.bpartners.api.model.subscription;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import static jakarta.persistence.CascadeType.ALL;
import static org.hibernate.type.SqlTypes.JSON;

@Entity(name = "user_subscription")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class Subscription {
  @Id private String id;
  private String e2Id;
  private boolean active;
  @JdbcTypeCode(JSON)
  private List<String> paymentMethods;
  @OneToOne(cascade = ALL)
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
