package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "user_subscription_commitment")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscriptionCommitment {
  @Id private String id;

  private String userId;

  @Column(name = "subscription_product_id")
  private String subscriptionPlanIdentifier;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private UserSubscriptionCommitmentDuration duration;

  private Instant approvalDatetime;

  private Instant commitmentStartDatetime;

  private Instant commitmentEndDatetime;

  @Column(updatable = false)
  private Instant creationDatetime;
}
