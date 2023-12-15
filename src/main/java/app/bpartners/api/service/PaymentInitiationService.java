package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.PAID;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.getSignature;
import static app.bpartners.api.service.PaymentScheduleService.PAYMENT_CREATED;
import static app.bpartners.api.service.PaymentScheduleService.paymentMessage;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
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
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.implementation.InvoiceRepositoryImpl;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.DateUtils;
import app.bpartners.api.service.utils.TemplateResolverUtils;
import java.security.Signature;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentInitiationService {
  public static final String PAYMENT_STATUS_CHANGED_TEMPLATE = "payment_status_changed";
  private final PaymentInitiationRepository repository;
  private final PaymentRequestJpaRepository jpaRepository;
  private final PaymentRequestMapper mapper;
  private final AccountService accountService;
  private final FintectureConf fintectureConf;
  private final SesService sesService;
  private final SesConf sesConf;
  private final UserRepository userRepository;
  private final InvoiceRepository invoiceRepository;
  private final InvoiceRepositoryImpl invoiceRepositoryImpl;
  private final SnsService snsService;

  @SneakyThrows
  @Transactional
  public void updatePaymentStatuses(Map<String, String> paymentStatusMap) {
    StringBuilder msgBuilder = new StringBuilder();
    List<HPaymentRequest> toSave = new ArrayList<>();
    paymentStatusMap.forEach(
        (sessionId, statusValue) -> {
          if (statusValue.equals(PAYMENT_CREATED)) {
            Optional<HPaymentRequest> optionalPayment = jpaRepository.findBySessionId(sessionId);
            if (optionalPayment.isEmpty()) {
              msgBuilder
                  .append("Unable to found payment with session_id=")
                  .append(sessionId)
                  .append(". ");
            } else {
              HPaymentRequest paymentRequest = optionalPayment.get();
              toSave.add(
                  paymentRequest.toBuilder()
                  .status(PAID)
                  .paymentMethod(PaymentMethod.BANK_TRANSFER)
                  .paymentStatusUpdatedAt(Instant.now())
                  .build());
            }
          } else {
            log.warn(
                "Payment(sessionId={}, statusValue={}) received successfully but not treated.",
                sessionId,
                statusValue);
          }
        });
    String msgValue = msgBuilder.toString();
    if (!msgValue.isEmpty()) {
      log.warn(msgValue);
    }
    if (!toSave.isEmpty()) {
      List<HPaymentRequest> savedPaidPayments = jpaRepository.saveAll(toSave);
      savedPaidPayments.stream()
          .filter(payment -> payment.getIdInvoice() != null)
          .toList()
          .forEach(payment -> {
            Invoice invoice = invoiceRepository.findById(payment.getIdInvoice());
            invoiceRepositoryImpl.processAsPdf(invoice);
            log.info("{} PDF is refreshed", invoice.describe());
          });
      log.info("Payment requests " + paymentMessage(savedPaidPayments) + " updated successfully");
      notifyByEmail(savedPaidPayments);
      notifyByMobileNotification(savedPaidPayments);
    }
  }

  private void notifyByMobileNotification(List<HPaymentRequest> paymentRequests) {
    Map<String, List<HPaymentRequest>> paymentsByUser = dispatchPaymentsByUser(paymentRequests);
    paymentsByUser.forEach(
        (idUser, payments) -> {
          User user = userRepository.getById(idUser);
          for (var payment : paymentRequests) {
            snsService.pushNotification(getNotificationTitle(payment), user);
          }
        });
  }

  private static Map<String, List<HPaymentRequest>> dispatchPaymentsByUser(
      List<HPaymentRequest> paymentRequests) {
    Map<String, List<HPaymentRequest>> paymentsByUser = new HashMap<>();
    for (HPaymentRequest payment : paymentRequests) {
      String idUser = payment.getIdUser();
      if (idUser != null) {
        if (!paymentsByUser.containsKey(idUser)) {
          List<HPaymentRequest> subList = new ArrayList<>();
          subList.add(payment);
          paymentsByUser.put(idUser, subList);
        } else {
          paymentsByUser.get(idUser).add(payment);
        }
      }
    }
    return paymentsByUser;
  }

  @SneakyThrows
  private void notifyByEmail(List<HPaymentRequest> paymentRequests) {
    for (var payment : paymentRequests) {
      User user = userRepository.getById(payment.getIdUser());
      AccountHolder accountHolder = user.getDefaultHolder();
      Invoice invoice =
          payment.getIdInvoice() == null ? null : invoiceRepository.getById(payment.getIdInvoice());
      Context context = new Context();
      context.setVariable("payment", payment);
      Fraction paymentAmount = parseFraction(payment.getAmount());
      context.setVariable("paymentAmount", paymentAmount);
      context.setVariable("accountHolder", accountHolder);
      context.setVariable("invoice", invoice);
      context.setVariable(
          "paymentDatetime", DateUtils.formatFrenchDatetime(payment.getPaymentStatusUpdatedAt()));
      String emailBody =
          TemplateResolverUtils.parseTemplateResolver(PAYMENT_STATUS_CHANGED_TEMPLATE, context);

      String recipient = accountHolder.getEmail();
      String cc = null;
      String bcc = sesConf.getAdminEmail();
      String subject = getNotificationTitle(payment);
      String htmlBody = emailBody;
      List<Attachment> attachments = List.of();
      sesService.sendEmail(recipient, cc, subject, htmlBody, attachments, bcc);
      log.info("Mail sent to {} after updating payment status id.{}", recipient, payment.getId());
    }
  }

  private static String getNotificationTitle(HPaymentRequest payment) {
    Fraction paymentAmount = parseFraction(payment.getAmount());
    return String.format(
        "Réception d'un nouveau paiement de %s € de la part de %s",
        paymentAmount.getCentsAsDecimal(), payment.getPayerName());
  }

  @SneakyThrows
  public void verifySignature(String signatureHeader, String sessionId, String paymentStatus) {
    String signatureAttribute = "signature=\"";
    int signatureAttributeIndex = signatureHeader.indexOf(signatureAttribute);
    String signatureValue =
        signatureHeader
            .substring(signatureAttributeIndex + 1)
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
          signatureValue,
          sessionId,
          paymentStatus,
          e.getMessage());
    }
  }

  public List<PaymentRedirection> initiatePayments(
      String accountId, List<PaymentInitiation> paymentInitiations) {
    checkAccountRequiredInfos(accountId);
    return repository.saveAll(paymentInitiations, null, null);
  }

  public PaymentRedirection initiateInvoicePayment(Invoice invoice) {
    if (Objects.equals(invoice.getTotalPriceWithVat(), new Fraction())) {
      return new PaymentRedirection();
    }
    CreatePaymentRegulation paymentReg = null;
    PaymentHistoryStatus paymentHistoryStatus = null;
    PaymentInitiation paymentInitiation =
        mapper.convertFromInvoice(
            String.valueOf(randomUUID()),
            invoice.getTitle(),
            invoice.getRealReference(),
            invoice,
            paymentReg,
            paymentHistoryStatus);
    return repository
        .saveAll(List.of(paymentInitiation), invoice.getId(), invoice.getUser())
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
