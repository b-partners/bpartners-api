package app.bpartners.api.model.subscription;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "user_subscription_upgrade_line")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscriptionUpgradeLine {
  @Id private String id;

  @Column(name = "line_order")
  private int lineOrder;

  @Enumerated(EnumType.STRING)
  private UpgradeLineKind kind;

  @Column(name = "subscription_product_id")
  private String subscriptionProductId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "billing_interval")
  private BillingInterval billingInterval;

  @Column(name = "period_start_date")
  private LocalDate periodStartDate;

  @Column(name = "period_end_date")
  private LocalDate periodEndDate;

  private int days;

  @Column(name = "reference_period_days")
  private int referencePeriodDays;

  @Column(name = "reference_price_in_cents_with_vat")
  private long referencePriceInCentsWithVat;

  @Column(name = "amount_in_cents_with_vat")
  private long amountInCentsWithVat;
}
