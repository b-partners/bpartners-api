package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "\"payment_request\"")
@Setter
@Getter
@ToString
@AllArgsConstructor
@Builder
@NoArgsConstructor
@EqualsAndHashCode
public class HPaymentRequest {
  @Id
  private String id;
  private String sessionId;
  private String accountId;
  private String idInvoice;
  private String label;
  private String paymentUrl;
  private String reference;
  private String amount;
  private String payerName;
  private String payerEmail;
  @CreationTimestamp
  private Instant createdDatetime;
}
