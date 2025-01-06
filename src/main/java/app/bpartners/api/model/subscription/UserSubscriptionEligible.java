package app.bpartners.api.model.subscription;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import lombok.*;

@Entity(name = "user_subscription_eligible")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscriptionEligible {
  @Id private String id;
  private String userId;

  @Getter(AccessLevel.NONE)
  private Integer trialPeriodDays;

  private LocalDate eligibleFrom;
  private Instant creationDatetime;

  public Integer getTrialPeriodDays() {
    return trialPeriodDays == null ? 0 : trialPeriodDays;
  }

  public LocalDate getLatestTrialPeriodDate() {
    return eligibleFrom.plusDays(getTrialPeriodDays());
  }
}
