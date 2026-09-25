package app.bpartners.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.*;

@Entity(name = "user_ignored_trial_period")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserIgnoredTrialPeriod {
  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @Column(updatable = false)
  private Instant creationDatetime;
}
