package app.bpartners.api.model.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "subscription_invoice_period")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class SubscriptionInvoicePeriod {
  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "invoice_id")
  private String invoiceId;

  @Column(name = "period_start_datetime")
  private Instant periodStartDatetime;

  @Column(name = "period_end_datetime")
  private Instant periodEndDatetime;

  @Column(updatable = false)
  private Instant creationDatetime;
}
