package app.bpartners.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.*;

@Entity(name = "user_subscription_trial")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscriptionTrial {
  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "subscription_product_id")
  private String subscriptionProductId;

  private Instant startedAt;

  private Instant expiresAt;

  @Column(updatable = false)
  private Instant creationDatetime;
}
