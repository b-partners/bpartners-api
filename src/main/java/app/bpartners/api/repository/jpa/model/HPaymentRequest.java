package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.model.PaymentRequest;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.hibernate.annotations.Type;

@Entity
@Table(name = "\"payment_request\"")
@Setter
@Getter
@ToString
@AllArgsConstructor
@Builder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode
public class HPaymentRequest implements Serializable {
  @Id
  private String id;
  private String sessionId;
  private String idUser;
  @Column(name = "id_invoice")
  private String idInvoice;
  private String label;
  private String paymentUrl;
  private String reference;
  private String amount;
  private String payerName;
  private String payerEmail;
  private LocalDate paymentDueDate;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private PaymentStatus status;
  @Column(name = "\"comment\"")
  private String comment;
  @CreationTimestamp
  private Instant createdDatetime;

  public HPaymentRequest(PaymentRequest p) {
    //TODO
  }
}
