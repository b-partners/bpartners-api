package app.bpartners.api.repository.jpa.model;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.model.PaymentHistoryStatus;
import app.bpartners.api.model.PaymentRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

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
  @Id private String id;
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

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod;

  @Column(name = "\"comment\"")
  private String comment;

  private Boolean userUpdated;
  private Instant paymentStatusUpdatedAt;
  @CreationTimestamp private Instant createdDatetime;

  public HPaymentRequest(PaymentRequest domain) {
    PaymentHistoryStatus historyStatus = domain.getPaymentHistoryStatus();
    this.id = domain.getId();
    this.sessionId = domain.getExternalId();
    this.idUser = domain.getIdUser();
    this.idInvoice = domain.getInvoiceId();
    this.label = domain.getLabel();
    this.paymentUrl = domain.getPaymentUrl();
    this.reference = domain.getReference();
    this.amount = domain.getAmount().toString();
    this.payerName = domain.getPayerName();
    this.payerEmail = domain.getPayerEmail();
    this.paymentDueDate = domain.getPaymentDueDate();
    this.comment = domain.getComment();
    this.status =
        historyStatus == null
            ? domain.getStatus()
            : (historyStatus.getStatus() == null
                ? PaymentStatus.UNPAID
                : historyStatus.getStatus());
    this.userUpdated = historyStatus == null ? null : historyStatus.getUserUpdated();
    this.paymentMethod = historyStatus == null ? null : historyStatus.getPaymentMethod();
    this.paymentStatusUpdatedAt = historyStatus == null ? null : domain.getCreatedDatetime();
    this.createdDatetime = domain.getCreatedDatetime();
  }
}
