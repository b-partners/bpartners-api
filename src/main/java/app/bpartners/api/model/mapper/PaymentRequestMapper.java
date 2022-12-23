package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.PaymentReqStatus;
import app.bpartners.api.endpoint.rest.model.PaymentTransferState;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.fintecture.model.Session;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.PaymentTransferState.UNKNOWN;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
@AllArgsConstructor
public class PaymentRequestMapper {
  public static final String PROVIDER_REQUIRED_STATUS = "provider_required";
  public static final String SCA_REQUIRED_STATUS = "sca_required";
  public static final String PAYMENT_WAITING_STATUS = "payment_waiting";
  public static final String PAYMENT_PENDING_STATUS = "payment_pending";
  public static final String PAYMENT_CANCELLED_STATUS = "payment_cancelled";
  public static final String PAYMENT_ERROR_STATUS = "payment_error";
  public static final String PAYMENT_UNSUCCESSFUL_STATUS = "payment_unsuccessful";
  public static final String PAYMENT_CREATED_STATUS = "payment_created";
  public static final String PROCESSING_STATE = "processing";
  public static final String PENDING_STATE = "pending";
  public static final String AUTHORIZED_STATE = "authorized";
  public static final String ACCEPTED_STATE = "accepted";
  public static final String SENT_STATE = "sent";
  public static final String COMPLETED_STATE = "completed";
  public static final String RECEIVED_STATE = "received";
  public static final String REJECTED_STATE = "rejected";
  private final PaymentRequestJpaRepository jpaRepository;
  private final AuthenticatedResourceProvider provider;
  private final FintecturePaymentInfoRepository paymentInfoRepository;

  public HPaymentRequest toEntity(
      FPaymentRedirection paymentRedirection,
      PaymentInitiation domain,
      String idInvoice) {
    return HPaymentRequest.builder()
        .id(domain.getId())
        .idInvoice(idInvoice)
        .accountId(provider.getAccount().getId())
        .sessionId(paymentRedirection.getMeta().getSessionId())
        .paymentUrl(paymentRedirection.getMeta().getUrl())
        .label(domain.getLabel())
        .payerEmail(domain.getPayerEmail())
        .payerName(domain.getPayerName())
        .reference(domain.getReference())
        .amount(domain.getAmount().toString())
        .build();
  }

  public PaymentRequest toDomain(HPaymentRequest entity) {
    Session session = paymentInfoRepository.getPaymentBySessionId(entity.getSessionId());
    Session.Attributes attributes = session == null ? null : session.getData().getAttributes();
    return PaymentRequest.builder()
        .id(entity.getId())
        .sessionId(entity.getSessionId())
        .invoiceId(entity.getIdInvoice())
        .paymentUrl(entity.getPaymentUrl())
        .status(session == null ? null : getPaymentReqStatus(session.getMeta().getStatus()))
        .transferState(attributes == null || attributes.getTransferState() == null
            ? UNKNOWN : getTransferState(attributes.getTransferState()))
        .paymentScheme(attributes == null ? null : attributes.getPaymentScheme())
        .accountId(entity.getAccountId())
        .label(entity.getLabel())
        .reference(entity.getReference())
        .payerName(entity.getPayerName())
        .payerEmail(entity.getPayerEmail())
        .amount(parseFraction(entity.getAmount()))
        .build();
  }

  private PaymentReqStatus getPaymentReqStatus(String status) {
    switch (status) {
      case PROVIDER_REQUIRED_STATUS:
      case SCA_REQUIRED_STATUS:
      case PAYMENT_WAITING_STATUS:
        return PaymentReqStatus.INITIALIZED;
      case PAYMENT_PENDING_STATUS:
        return PaymentReqStatus.PROCESSING;
      case PAYMENT_CANCELLED_STATUS:
        return PaymentReqStatus.CANCELED;
      case PAYMENT_ERROR_STATUS:
        return PaymentReqStatus.ERROR;
      case PAYMENT_UNSUCCESSFUL_STATUS:
        return PaymentReqStatus.REJECTED;
      case PAYMENT_CREATED_STATUS:
        return PaymentReqStatus.CREATED;
      default:
        log.warn("Unknown payment request status " + status);
        return PaymentReqStatus.UNKNOWN;
    }
  }

  private PaymentTransferState getTransferState(String state) {
    switch (state) {
      case PROCESSING_STATE:
      case PENDING_STATE:
      case AUTHORIZED_STATE:
      case ACCEPTED_STATE:
        //TODO: The payment is being processed
        // and the bank does not return settlement information
      case SENT_STATE:
        return PaymentTransferState.PROCESSING;
      case COMPLETED_STATE:
      case RECEIVED_STATE: //TODO: Only if a Fintecture Payment Account exists
        return PaymentTransferState.COMPLETED;
      case REJECTED_STATE:
        return PaymentTransferState.REJECTED;
      default:
        log.warn("Unknown payment transfert state " + state);
        return UNKNOWN;
    }
  }
}
