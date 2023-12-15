package app.bpartners.api.model;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.PAID;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Data
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
  private PaymentHistoryStatus paymentHistoryStatus;
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
    this.paymentHistoryStatus =
        entity.getPaymentMethod() == null && entity.getStatus() == null
                || entity.getStatus() == PaymentStatus.UNPAID
                    && entity.getPaymentStatusUpdatedAt() == null
                    && entity.getUserUpdated() == null
            ? null
            : PaymentHistoryStatus.builder()
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .updatedAt(
                    entity.getPaymentStatusUpdatedAt() == null
                        ? entity.getCreatedDatetime()
                        : entity.getPaymentStatusUpdatedAt())
                .userUpdated(entity.getUserUpdated())
                .build();
  }

  public String getStamp() {
    PaymentHistoryStatus historyStatus = this.paymentHistoryStatus;
    if (historyStatus == null || historyStatus.getStatus() == null) {
      return null;
    }
    if (historyStatus.getStatus() == PAID) {
      return addStamp(historyStatus.getPaymentMethod());
    }
    return null;
  }

  @SneakyThrows
  public static String addStamp(PaymentMethod paymentMethod) {
    if (paymentMethod == null) {
      return null;
    }
    String path;
    switch (paymentMethod) {
      case CASH:
        path = "static/stamp/cash-no-bg.png";
        break;
      case BANK_TRANSFER:
        path = "static/stamp/bank-no-bg.png";
        break;
      case CHEQUE:
        path = "static/stamp/cheque-no-bg.png";
        break;
      case UNKNOWN:
        path = "static/stamp/paid-no-bg.png";
        break;
      case MULTIPLE:
        throw new NotImplementedException("MULTIPLE method not supported");
      default:
        log.warn("Unable to get stamp for unknown payment method {} ", paymentMethod);
        return null;
    }
    InputStream is = new ClassPathResource(path).getInputStream();
    byte[] bytes = is.readAllBytes();
    return Base64.getEncoder().encodeToString(bytes);
  }
}
