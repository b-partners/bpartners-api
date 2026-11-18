package app.bpartners.api.model.subscription;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "temp_subscription_termination")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class TempSubscriptionTermination {
  @Id private String id;

  @Column(name = "user_subscription_e2_id")
  private String userSubscriptionE2Id;

  private Instant subscriptionEndDatetime;

  @Column(name = "current_period_end_date")
  private LocalDate currentPeriodEndDate;

  @Column(name = "amount_in_cents_with_vat")
  private long amountInCentsWithVat;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private SubscriptionTerminationStatus status;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "credit_note_invoice_id")
  private String creditNoteInvoiceId;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "update_datetime")
  private Instant updateDatetime;
}
