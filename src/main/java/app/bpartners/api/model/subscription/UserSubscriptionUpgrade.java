package app.bpartners.api.model.subscription;

import static java.util.Comparator.comparing;
import static java.util.UUID.randomUUID;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "user_subscription_upgrade")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserSubscriptionUpgrade {
  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "from_subscription_product_id")
  private String fromSubscriptionProductId;

  @Column(name = "to_subscription_product_id")
  private String toSubscriptionProductId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "from_billing_interval")
  private BillingInterval fromBillingInterval;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "to_billing_interval")
  private BillingInterval toBillingInterval;

  private Instant upgradeDatetime;

  @Column(name = "old_coverage_end_datetime")
  private Instant oldCoverageEndDatetime;

  @Column(name = "new_commitment_end_datetime")
  private Instant newCommitmentEndDatetime;

  @Column(name = "proration_amount_in_cents_with_vat")
  private long prorationAmountInCentsWithVat;

  @Column(name = "stripe_proration_invoice_id")
  private String stripeProrationInvoiceId;

  @Column(name = "old_stripe_subscription_id")
  private String oldStripeSubscriptionId;

  @Column(name = "payment_url")
  private String paymentUrl;

  @Column(name = "payment_expiration_datetime")
  private Instant paymentExpirationDatetime;

  @Column(name = "new_stripe_subscription_id")
  private String newStripeSubscriptionId;

  @Column(name = "credit_note_invoice_id")
  private String creditNoteInvoiceId;

  @Column(name = "new_invoice_id")
  private String newInvoiceId;

  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "user_subscription_upgrade_id")
  @Getter(AccessLevel.NONE)
  private List<UserSubscriptionUpgradeStatusHistory> statusHistory;

  @Column(updatable = false)
  private Instant creationDatetime;

  public List<UserSubscriptionUpgradeStatusHistory> getStatusHistory() {
    return statusHistory == null
        ? List.of()
        : statusHistory.stream()
            .sorted(comparing(UserSubscriptionUpgradeStatusHistory::getCreationDatetime))
            .toList();
  }

  public SubscriptionUpgradeStatus getStatus() {
    var history = getStatusHistory();
    return history.isEmpty() ? null : history.getLast().getStatus();
  }

  public static class UserSubscriptionUpgradeBuilder {
    public UserSubscriptionUpgradeBuilder status(SubscriptionUpgradeStatus status) {
      var history = new ArrayList<UserSubscriptionUpgradeStatusHistory>();
      if (statusHistory != null) {
        history.addAll(statusHistory);
      }
      history.add(
          UserSubscriptionUpgradeStatusHistory.builder()
              .id(randomUUID().toString())
              .userSubscriptionUpgradeId(id)
              .status(status)
              .creationDatetime(Instant.now())
              .build());
      statusHistory = history;
      return this;
    }
  }
}
