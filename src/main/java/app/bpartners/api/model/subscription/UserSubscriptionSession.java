package app.bpartners.api.model.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
}
