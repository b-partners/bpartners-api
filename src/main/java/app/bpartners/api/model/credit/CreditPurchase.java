package app.bpartners.api.model.credit;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "credit_purchase")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class CreditPurchase {
  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private CreditPurchaseType type;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "credit_pack_id")
  private CreditPack creditPack;

  private Integer quantity;

  private Long credits;

  @Column(name = "credit_unit_price_in_cents_without_vat")
  private Long creditUnitPriceInCentsWithoutVat;

  @Column(name = "amount_in_cents_without_vat")
  private Long amountInCentsWithoutVat;

  @Column(name = "amount_in_cents_with_vat")
  private Long amountInCentsWithVat;

  @Column(name = "vat_percent")
  private Long vatPercent;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private CreditPurchaseStatus status;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private CreditPurchaseOrigin origin;

  @Column(name = "redirection_url")
  private String redirectionUrl;

  @Column(name = "redirection_success_url")
  private String redirectionSuccessUrl;

  @Column(name = "redirection_failure_url")
  private String redirectionFailureUrl;

  @Column(name = "credit_transaction_id")
  private String creditTransactionId;

  @Column(name = "invoice_id")
  private String invoiceId;

  @Column(name = "completion_datetime")
  private Instant completionDatetime;

  @Column(name = "credits_expiration_datetime")
  private Instant creditsExpirationDatetime;

  @Column(updatable = false)
  private Instant creationDatetime;

  public CreditUnitPrice unitPriceApplied() {
    return new CreditUnitPrice(
        creditUnitPriceInCentsWithoutVat == null ? 0L : creditUnitPriceInCentsWithoutVat,
        vatPercent == null ? 0L : vatPercent);
  }
}
