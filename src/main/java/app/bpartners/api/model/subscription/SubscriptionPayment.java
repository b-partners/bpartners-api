package app.bpartners.api.model.subscription;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "subscription_payment")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class SubscriptionPayment {
  public static final String DEFAULT_LABEL = "Abonnement";

  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "stripe_invoice_id")
  private String stripeInvoiceId;

  @Column(name = "stripe_subscription_id")
  private String stripeSubscriptionId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "subscription_product_id")
  private SubscriptionProduct subscriptionProduct;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "billing_interval")
  private BillingInterval billingInterval;

  private String label;

  @Column(name = "amount_in_cents_without_vat")
  private Long amountInCentsWithoutVat;

  @Column(name = "amount_in_cents_with_vat")
  private Long amountInCentsWithVat;

  @Column(name = "vat_percent")
  private Long vatPercent;

  @Column(name = "period_start_datetime")
  private Instant periodStartDatetime;

  @Column(name = "period_end_datetime")
  private Instant periodEndDatetime;

  @Column(name = "payment_datetime")
  private Instant paymentDatetime;

  @Column(name = "invoice_id")
  private String invoiceId;

  @Column(updatable = false)
  private Instant creationDatetime;

  public Instant getCreationDatetime() {
    return creationDatetime == null ? null : creationDatetime.truncatedTo(ChronoUnit.MILLIS);
  }

  public Instant getPaymentDatetime() {
    return paymentDatetime == null ? null : paymentDatetime.truncatedTo(ChronoUnit.MILLIS);
  }

  public String paymentLabel() {
    if (label != null && !label.isBlank()) {
      return label;
    }
    return planName();
  }

  public String planName() {
    return subscriptionProduct == null || subscriptionProduct.getName() == null
        ? DEFAULT_LABEL
        : subscriptionProduct.getName();
  }

  public long amountInCentsWithoutVatOrZero() {
    return amountInCentsWithoutVat == null ? 0L : amountInCentsWithoutVat;
  }

  public long vatPercentOrZero() {
    return vatPercent == null ? 0L : vatPercent;
  }
}
