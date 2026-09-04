package app.bpartners.api.model;

import static jakarta.persistence.FetchType.LAZY;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
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

  @OneToMany(fetch = LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "user_subscription_commitment_id")
  @Getter(AccessLevel.NONE)
  private List<UserSubscriptionCommitmentAutoRenewalStatusHistory> autoRenewalStatusHistory;

  private Instant approvalDatetime;

  private Instant commitmentStartDatetime;

  private Instant commitmentEndDatetime;

  @Column(updatable = false)
  private Instant creationDatetime;

  public EnableStatus getAutomaticRenewalStatus() {
    return autoRenewalStatusHistory == null || autoRenewalStatusHistory.isEmpty()
        ? null
        : autoRenewalStatusHistory.stream()
            .sorted(
                comparing(
                        UserSubscriptionCommitmentAutoRenewalStatusHistory::getCreationDatetime,
                        naturalOrder())
                    .reversed())
            .toList()
            .getFirst()
            .getAutoRenewalStatus();
  }
}
