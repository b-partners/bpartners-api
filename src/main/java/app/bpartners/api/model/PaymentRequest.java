package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
public class PaymentRequest {
  private String id;
  private String idUser;
  private String externalId;
  private String paymentUrl;
  private String invoiceId;
  private String label;
  private String reference;
  private String payerName;
  private String payerEmail;
  private Fraction amount;
  private LocalDate paymentDueDate;
  private PaymentStatus status;
  private String comment;
  private Instant createdDatetime;

  public PaymentRequest(HPaymentRequest entity) {
    this.id = entity.getId();
    this.externalId = entity.getSessionId();
    this.idUser = entity.getIdUser();
    this.invoiceId = entity.getIdInvoice();
    this.label = entity.getLabel();
    this.paymentUrl = entity.getPaymentUrl();
    this.reference = entity.getReference();
    this.amount = parseFraction(entity.getAmount());
    this.payerName = entity.getPayerName();
    this.payerEmail = entity.getPayerEmail();
    this.paymentDueDate = entity.getPaymentDueDate();
    this.status = entity.getStatus();
    this.comment = entity.getComment();
    this.createdDatetime = entity.getCreatedDatetime();
  }
}
