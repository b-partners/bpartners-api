package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentHistoryStatus;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.time.Instant;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
public class PaymentRequestMapper {
  public HPaymentRequest toEntity(PaymentRequest domain, HPaymentRequest existing) {
    PaymentHistoryStatus paymentHistoryStatus = domain.getPaymentHistoryStatus();
    Instant createdDatetime = existing == null ? Instant.now() :
        existing.getCreatedDatetime();
    return HPaymentRequest.builder()
        .id(domain.getId())
        .idInvoice(domain.getInvoiceId())
        .idUser(domain.getIdUser())
        .sessionId(domain.getExternalId())
        .paymentUrl(domain.getPaymentUrl())
        .label(domain.getLabel())
        .comment(domain.getComment())
        .payerEmail(domain.getPayerEmail())
        .payerName(domain.getPayerName())
        .reference(domain.getReference())
        .paymentDueDate(domain.getPaymentDueDate())
        .amount(domain.getAmount().toString())
        .createdDatetime(createdDatetime)
        .status(domain.getStatus())
        .paymentMethod(paymentHistoryStatus == null ? null
            : paymentHistoryStatus.getPaymentMethod())
        .paymentStatusUpdatedAt(
            paymentHistoryStatus == null ? createdDatetime
                : paymentHistoryStatus.getUpdatedAt())
        .userUpdated(paymentHistoryStatus == null ? null
            : paymentHistoryStatus.getUserUpdated())
        .paymentStatusUpdatedAt(
            paymentHistoryStatus == null ? null :
                paymentHistoryStatus.getUpdatedAt()
        ).build();
  }

  public HPaymentRequest toEntity(
      FPaymentRedirection paymentRedirection, PaymentInitiation domain, String idInvoice) {
    PaymentHistoryStatus historyStatus = domain.getPaymentHistoryStatus();
    Instant createdDatetime = Instant.now();
    return HPaymentRequest.builder()
        .id(domain.getId())
        .idInvoice(idInvoice)
        .idUser(AuthProvider.getAuthenticatedUserId())
        .sessionId(paymentRedirection == null ? null
            : paymentRedirection.getMeta().getSessionId())
        .paymentUrl(paymentRedirection == null ? null
            : paymentRedirection.getMeta().getUrl())
        .label(domain.getLabel())
        .comment(domain.getComment())
        .payerEmail(domain.getPayerEmail())
        .payerName(domain.getPayerName())
        .reference(domain.getReference())
        .paymentDueDate(domain.getPaymentDueDate())
        .amount(domain.getAmount().toString())
        .createdDatetime(createdDatetime)
        .status(domain.getStatus() == null ? PaymentStatus.UNPAID : domain.getStatus())
        .paymentStatusUpdatedAt(
            historyStatus == null ? createdDatetime : historyStatus.getUpdatedAt())
        .build();
  }

  public PaymentInitiation convertFromInvoice(
      String paymentInitiationId, String label, String reference, Invoice invoice,
      CreatePaymentRegulation payment, PaymentHistoryStatus paymentHistoryStatus) {
    Fraction totalPriceWithVat = invoice.getTotalPriceWithVat();
    return PaymentInitiation.builder()
        .id(paymentInitiationId)
        .reference(reference)
        .label(label)
        .amount(
            payment != null
                ? payment.getAmountOrPercent(totalPriceWithVat)
                : totalPriceWithVat)
        //TODO: use customerName and customerEmail when overriding
        .comment(payment != null ? payment.getComment()
            : null)
        .payerName(invoice.getCustomer() == null
            ? null : invoice.getCustomer().getFullName())
        .payerEmail(invoice.getCustomer() == null
            ? null : invoice.getCustomer().getEmail())
        .paymentDueDate(payment != null
            ? payment.getMaturityDate()
            : null)
        .successUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .failureUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .paymentHistoryStatus(paymentHistoryStatus)
        .build();
  }

  public CreatePaymentRegulation toPaymentRegulation(PaymentRequest payment, Fraction percent) {
    PaymentHistoryStatus paymentHistoryStatus = payment.getPaymentHistoryStatus();
    return CreatePaymentRegulation.builder()
        .paymentRequest(PaymentRequest.builder()
            .id(payment.getId())
            .idUser(payment.getIdUser())
            .externalId(payment.getExternalId())
            .label(payment.getLabel())
            .amount(payment.getAmount())
            .paymentUrl(payment.getPaymentUrl())
            .reference(payment.getReference())
            .payerName(payment.getPayerName())
            .payerEmail(payment.getPayerEmail())
            .paymentDueDate(payment.getPaymentDueDate())
            .createdDatetime(payment.getCreatedDatetime())
            .status(payment.getStatus())
            .comment(payment.getComment())
            .paymentHistoryStatus(PaymentHistoryStatus.builder()
                .paymentMethod(paymentHistoryStatus == null ? null
                    : paymentHistoryStatus.getPaymentMethod())
                .status(paymentHistoryStatus == null ? null
                    : paymentHistoryStatus.getStatus())
                .userUpdated(paymentHistoryStatus == null ? null
                    : paymentHistoryStatus.getUserUpdated())
                .updatedAt(paymentHistoryStatus == null ? null
                    : paymentHistoryStatus.getUpdatedAt())
                .build())
            .build())
        .percent(percent)
        .comment(payment.getComment())
        .maturityDate(payment.getPaymentDueDate())
        .initiatedDatetime(payment.getCreatedDatetime())
        .build();
  }

  public PaymentRequest toDomain(HPaymentRequest entity) {
    return PaymentRequest.builder()
        .id(entity.getId())
        .invoiceId(entity.getIdInvoice())
        .idUser(entity.getIdUser())
        .externalId(entity.getSessionId())
        .paymentUrl(entity.getPaymentUrl())
        .label(entity.getLabel())
        .comment(entity.getComment())
        .payerEmail(entity.getPayerEmail())
        .payerName(entity.getPayerName())
        .reference(entity.getReference())
        .paymentDueDate(entity.getPaymentDueDate())
        .amount(parseFraction(entity.getAmount()))
        .createdDatetime(Instant.now())
        .status(entity.getStatus())
        .paymentHistoryStatus(
            entity.getPaymentMethod() == null
                && entity.getStatus() == null
                && entity.getUserUpdated() == null
                && entity.getPaymentStatusUpdatedAt() == null ? null
                : PaymentHistoryStatus.builder()
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .userUpdated(entity.getUserUpdated())
                .updatedAt(entity.getPaymentStatusUpdatedAt())
                .build())
        .build();
  }
}
