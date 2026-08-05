package app.bpartners.api.model.subscription;

import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "subscription_product")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class SubscriptionProduct {
  public static final long DEFAULT_FREE_USAGE_THRESHOLD = 20L;

  public static final long DEFAULT_OVERAGE_UNIT_PRICE_IN_CENTS = 200L;

  public static final long DEFAULT_VAT_PERCENT = 2000L;

  @Id private String id;

  @Column(name = "e2_id")
  private String e2Id;

  private String name;
  private String description;

  @JdbcTypeCode(JSON)
  private List<String> features;

  private String imageUrl;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private SubscriptionType type;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private SubscriptionConsumptionType consumptionTypeAttached;

  private Long vatPercent;

  private Long priceInCentsWithoutVat;

  private boolean mostChosen;

  private boolean deprecated;

  private Integer displayPosition;

  private Instant creationDatetime;

  @Column(name = "plan_code")
  private String planCode;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "billing_type")
  private SubscriptionBillingType billingType;

  @Column(name = "free_usage_threshold")
  private Long freeUsageThreshold;

  @Column(name = "overage_unit_price_in_cents")
  private Long overageUnitPriceInCents;

  @Column(name = "trial_period_days")
  private Integer trialPeriodDays;

  @Column(name = "annual_discount_percent")
  private Integer annualDiscountPercent;

  @Column(name = "metered_product_id")
  private String meteredProductId;

  public Long getPriceInCentsWithVat() {
    if (priceInCentsWithoutVat == null || vatPercent == null) {
      return null;
    }
    // vatPercent is expressed in basis points of 10_000 (2000 = 20%).
    // TTC = HT * (10_000 + vatPercent) / 10_000, rounded half up to whole cents.
    var numerator = priceInCentsWithoutVat * (10_000L + vatPercent);
    return (numerator + 5_000L) / 10_000L;
  }

  public static Long priceInCentsWithoutVatFrom(Long priceInCentsWithVat, Long vatPercent) {
    if (priceInCentsWithVat == null || vatPercent == null) {
      return null;
    }
    var denominator = 10_000L + vatPercent;
    var numerator = priceInCentsWithVat * 10_000L;
    return (numerator + denominator / 2) / denominator;
  }

  public long freeUsageThresholdOrDefault() {
    return freeUsageThreshold == null ? DEFAULT_FREE_USAGE_THRESHOLD : freeUsageThreshold;
  }

  public long overageUnitPriceInCentsOrDefault() {
    return overageUnitPriceInCents == null
        ? DEFAULT_OVERAGE_UNIT_PRICE_IN_CENTS
        : overageUnitPriceInCents;
  }
}
