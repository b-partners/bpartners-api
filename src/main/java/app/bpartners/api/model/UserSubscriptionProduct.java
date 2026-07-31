package app.bpartners.api.model;

import app.bpartners.api.model.subscription.SubscriptionProduct;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

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

  private Instant subscriptionStartDatetime;

  private Instant subscriptionEndDatetime;

  @Column(updatable = false)
  private Instant creationDatetime;
}
