package app.bpartners.api.model.credit;

import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
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

@Entity(name = "credit_purchase")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class CreditPurchase {
  private static final String CUSTOM_INVOICE_LINE_LABEL = "Crédits d'analyse à l'unité";
  private static final String CUSTOM_PAYMENT_LABEL_SUFFIX = " crédits d'analyse à l'unité";

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

  public Instant getCreationDatetime() {
    return creationDatetime == null ? null : creationDatetime.truncatedTo(ChronoUnit.MILLIS);
  }

  public Instant getCompletionDatetime() {
    return completionDatetime == null ? null : completionDatetime.truncatedTo(ChronoUnit.MILLIS);
  }

  public boolean isCustomPurchase() {
    return CUSTOM.equals(type);
  }

  public int packQuantity() {
    return quantity == null || quantity < 1 ? 1 : quantity;
  }

  public Long creditsPerPack() {
    if (creditPack != null && creditPack.getCredits() != null) {
      return creditPack.getCredits();
    }
    return credits == null ? null : credits / packQuantity();
  }

  public String paymentLabel() {
    var packDescription = packDescription();
    if (packDescription != null) {
      return packDescription;
    }
    return isCustomPurchase() ? credits + CUSTOM_PAYMENT_LABEL_SUFFIX : packLabel(credits);
  }

  public String invoiceLineLabel() {
    return isCustomPurchase() ? CUSTOM_INVOICE_LINE_LABEL : packLabel(creditsPerPack());
  }

  private String packLabel(Long packCredits) {
    return packCredits == null
        ? "Pack de crédits d'analyse"
        : "Pack de " + packCredits + " crédits d'analyse";
  }

  private String packDescription() {
    return creditPack == null ? null : creditPack.getDescription();
  }

  public CreditUnitPrice unitPriceApplied() {
    return new CreditUnitPrice(
        creditUnitPriceInCentsWithoutVat == null ? 0L : creditUnitPriceInCentsWithoutVat,
        vatPercent == null ? 0L : vatPercent);
  }
}
