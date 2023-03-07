package app.bpartners.api.repository.jpa.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import app.bpartners.api.model.PaymentRequest;
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
public class HPaymentRequest implements Serializable {
  @Id
  private String id;
  private String sessionId;
  private String accountId;
  @Column(name = "id_invoice")
  private String idInvoice;
  private String label;
  private String paymentUrl;
  private String reference;
  private String amount;
  private String payerName;
  private String payerEmail;
  private LocalDate paymentDueDate;
  @CreationTimestamp
  private Instant createdDatetime;

  public HPaymentRequest(PaymentRequest p) {
    //TODO
  }
}
