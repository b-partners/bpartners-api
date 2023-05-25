package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestMapper {

  public HPaymentRequest toEntity(
      FPaymentRedirection paymentRedirection, PaymentInitiation domain, String idInvoice) {
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
        .createdDatetime(Instant.now())
        .status(domain.getStatus() == null ? PaymentStatus.UNPAID : domain.getStatus())
        .build();
  }

  public PaymentInitiation convertFromInvoice(
      String paymentInitiationId, Invoice invoice, CreatePaymentRegulation payment) {
    Fraction totalPriceWithVat = invoice.getTotalPriceWithVat();
    return PaymentInitiation.builder()
        .id(paymentInitiationId)
        .reference(invoice.getRealReference())
        .label(invoice.getTitle())
        .amount(
            payment != null
                ? payment.getAmountOrPercent(totalPriceWithVat)
                : totalPriceWithVat)
        //TODO: use customerName and customerEmail when overriding
        .comment(payment != null ? payment.getComment()
            : null)
        .payerName(invoice.getCustomer() == null
            ? null : invoice.getCustomer().getName())
        .payerEmail(invoice.getCustomer() == null
            ? null : invoice.getCustomer().getEmail())
        .paymentDueDate(payment != null
            ? payment.getMaturityDate()
            : null)
        .successUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .failureUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .build();
  }

  public CreatePaymentRegulation toPaymentRegulation(PaymentRequest payment, Fraction percent) {
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
            .build())
        .percent(percent)
        .comment(payment.getComment())
        .maturityDate(payment.getPaymentDueDate())
        .initiatedDatetime(payment.getCreatedDatetime())
        .build();
  }

}
