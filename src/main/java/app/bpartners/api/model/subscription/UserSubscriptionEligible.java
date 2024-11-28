package app.bpartners.api.model.subscription;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
}
