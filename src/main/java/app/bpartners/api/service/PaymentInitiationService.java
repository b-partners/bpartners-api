package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentHistoryStatus;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.security.Signature;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.PAID;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.getSignature;
import static app.bpartners.api.service.PaymentScheduleService.PAYMENT_CREATED;
import static app.bpartners.api.service.PaymentScheduleService.paymentMessage;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentInitiationService {
  private final PaymentInitiationRepository repository;
  private final PaymentRequestJpaRepository jpaRepository;
  private final PaymentRequestMapper mapper;
  private final AccountService accountService;
  private final FintectureConf fintectureConf;

  @SneakyThrows
  public void updatePaymentStatuses(Map<String, String> paymentStatusMap) {
    StringBuilder msgBuilder = new StringBuilder();
    List<HPaymentRequest> toSave = new ArrayList<>();
    paymentStatusMap.forEach(
        (sessionId, statusValue) -> {
          if (statusValue.equals(PAYMENT_CREATED)) {
            Optional<HPaymentRequest> optionalPayment = jpaRepository.findBySessionId(sessionId);
            if (optionalPayment.isEmpty()) {
              msgBuilder.append("Unable to found payment with session_id=")
                  .append(sessionId)
                  .append(". ");
            } else {
              HPaymentRequest paymentRequest = optionalPayment.get();
              toSave.add(paymentRequest.toBuilder()
                  .status(PAID)
                  .paymentStatusUpdatedAt(Instant.now())
                  .build());
            }
          } else {
            log.warn("Payment(sessionId={}, statusValue={}) received successfully but not treated.",
                sessionId, statusValue);
          }
        }
    );
    String msgValue = msgBuilder.toString();
    if (!msgValue.isEmpty()) {
      log.warn(msgValue);
    }
    if (!toSave.isEmpty()) {
      List<HPaymentRequest> savedPaidPayments = jpaRepository.saveAll(toSave);
      log.info("Payment requests " + paymentMessage(savedPaidPayments) + " updated successfully");
    }
  }

  @SneakyThrows
  public void verifySignature(String signatureHeader, String sessionId, String paymentStatus) {
    String signatureAttribute = "signature=\"";
    int signatureAttributeIndex = signatureHeader.indexOf(signatureAttribute);
    String signatureValue =
        signatureHeader.substring(signatureAttributeIndex + 1)
            .replaceAll(signatureAttribute, "")
            .replaceAll("\"", "");
    Signature sign = getSignature(fintectureConf.getPrivateKey(), signatureValue);
    byte[] signatureAsBytes = Base64.getDecoder().decode(signatureValue);
    try {
      sign.verify(signatureAsBytes);
    } catch (Exception e) {
      log.warn(
          "Unable to verify signature {} when trying to handle payment status change "
              + "of Payment(sessionId={}, status={}). Exception thrown : {}",
          signatureValue, sessionId, paymentStatus, e.getMessage());
    }
  }

  public List<PaymentRedirection> initiatePayments(
      String accountId, List<PaymentInitiation> paymentInitiations) {
    checkAccountRequiredInfos(accountId);
    return repository.saveAll(paymentInitiations, null, null);
  }

  public PaymentRedirection initiateInvoicePayment(
      Invoice invoice) {
    if (Objects.equals(invoice.getTotalPriceWithVat(), new Fraction())) {
      return new PaymentRedirection();
    }
    CreatePaymentRegulation paymentReg = null;
    PaymentHistoryStatus paymentHistoryStatus = null;
    PaymentInitiation paymentInitiation = mapper.convertFromInvoice(
        String.valueOf(randomUUID()),
        invoice.getTitle(),
        invoice.getRealReference(),
        invoice,
        paymentReg,
        paymentHistoryStatus);
    return repository.saveAll(List.of(paymentInitiation), invoice.getId(), invoice.getUser())
        .get(0);
  }

  public List<PaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiations, String invoiceId, InvoiceStatus status) {
    if (status == InvoiceStatus.CONFIRMED || status == InvoiceStatus.PAID) {
      return List.of();
    }
    return repository.retrievePaymentEntities(paymentInitiations, invoiceId).stream()
        .map(PaymentRequest::new)
        .collect(Collectors.toList());
  }

  public List<PaymentRequest> retrievePaymentEntitiesWithUrl(
      List<PaymentInitiation> paymentInitiations, String invoiceId, User user) {
    return repository.retrievePaymentEntitiesWithUrl(paymentInitiations, invoiceId, user).stream()
        .map(PaymentRequest::new)
        .collect(Collectors.toList());
  }

  private void checkAccountRequiredInfos(String accountId) {
    Account account = accountService.getAccountById(accountId);
    StringBuilder builder = new StringBuilder();
    if (account.getBic() == null) {
      builder.append("Bic is mandatory for initiating payments. ");
    }
    if (account.getIban() == null) {
      builder.append("Iban is mandatory for initiating payments. ");
    }
    String message = builder.toString();
    if (!message.isEmpty()) {
      throw new BadRequestException(message);
    }
  }
}
