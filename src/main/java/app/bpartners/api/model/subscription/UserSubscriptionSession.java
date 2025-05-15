package app.bpartners.api.model.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.*;

@Entity(name = "user_subscription_session")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscriptionSession {
  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "session_id")
  private String sessionId;

  @Column(name = "session_mode")
  private SessionMode sessionMode;

  @Column(name = "subscription_schedule_id")
  private String subscriptionScheduleId;

  @Column(name = "set_up_until")
  private LocalDate setUpUntil;

  @Column(name = "is_cancelled")
  private boolean isCancelled;
}
