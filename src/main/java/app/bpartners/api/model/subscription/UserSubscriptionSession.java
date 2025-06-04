package app.bpartners.api.model.subscription;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "session_mode")
  private SessionMode sessionMode;

  @Column(name = "subscription_schedule_id")
  private String subscriptionScheduleId;

  @Column(name = "trial_until")
  private LocalDate trialUntil;

  @Column(name = "is_cancelled")
  private boolean isCancelled;
}
