package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "user_subscription_commitment_auto_renewal_status_history")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscriptionCommitmentAutoRenewalStatusHistory {
  @Id private String id;

  // the parent UserSubscriptionCommitment owns this FK through its @OneToMany @JoinColumn, so the
  // logical column name here must match it to avoid a duplicate-mapping error at boot
  @Column(name = "user_subscription_commitment_id")
  private String userSubscriptionCommitmentId;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private EnableStatus autoRenewalStatus;

  @Column(updatable = false)
  private Instant creationDatetime;
}
