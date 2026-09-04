package app.bpartners.api.model;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "user_subscription_product")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscriptionProduct {
  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "subscription_product_id")
  public SubscriptionProduct subscriptionProduct;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "billing_interval")
  private BillingInterval billingInterval;

  private Instant subscriptionStartDatetime;

  private Instant subscriptionEndDatetime;

  @Column(updatable = false)
  private Instant creationDatetime;
}
