package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
@AllArgsConstructor
public class PaymentRequestMapper {
  private final AuthenticatedResourceProvider provider;

  public HPaymentRequest toEntity(
      FPaymentRedirection paymentRedirection,
      PaymentInitiation domain,
      String idInvoice) {
    return HPaymentRequest.builder()
        .id(domain.getId())
        .idInvoice(idInvoice)
        .accountId(provider.getAccount().getId())
        .sessionId(paymentRedirection == null ? null
            : paymentRedirection.getMeta().getSessionId())
        .paymentUrl(paymentRedirection == null ? null
            : paymentRedirection.getMeta().getUrl())
        .label(domain.getLabel())
        .payerEmail(domain.getPayerEmail())
        .payerName(domain.getPayerName())
        .reference(domain.getReference())
        .paymentDueDate(domain.getPaymentDueDate())
        .amount(domain.getAmount().toString())
        .build();
  }

  public PaymentRequest toDomain(HPaymentRequest entity) {
    return PaymentRequest.builder()
        .id(entity.getId())
        .sessionId(entity.getSessionId())
        .invoiceId(entity.getIdInvoice())
        .paymentUrl(entity.getPaymentUrl())
        .accountId(entity.getAccountId())
        .label(entity.getLabel())
        .reference(entity.getReference())
        .payerName(entity.getPayerName())
        .payerEmail(entity.getPayerEmail())
        .amount(parseFraction(entity.getAmount()))
        .build();
  }

  public PaymentInitiation convertFromInvoice(
      String paymentInitiationId, Invoice invoice,
      Fraction totalPriceWithVat, CreatePaymentRegulation payment) {
    return PaymentInitiation.builder()
        .id(paymentInitiationId)
        .reference(invoice.getRef())
        .label(payment != null
            ? payment.getComment()
            : invoice.getTitle())
        .amount(
            payment != null
                ? payment.getAmountOrPercent(totalPriceWithVat)
                : totalPriceWithVat)
        //TODO: use customerName and customerEmail when overriding
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
}
