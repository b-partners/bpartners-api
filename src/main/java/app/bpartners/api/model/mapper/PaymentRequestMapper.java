package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentRequestMapper {
  private final PaymentRequestJpaRepository jpaRepository;
  private final AuthenticatedResourceProvider provider;

  public HPaymentRequest toEntity(
      PaymentRedirection paymentRedirection,
      app.bpartners.api.model.PaymentInitiation paymentInitiation,
      String idInvoice) {
    return HPaymentRequest.builder()
        .id(paymentInitiation.getId())
        .idInvoice(idInvoice)
        .accountId(provider.getAccount().getId())
        .sessionId(paymentRedirection.getMeta().getSessionId())
        .label(paymentInitiation.getLabel())
        .payerEmail(paymentInitiation.getPayerEmail())
        .payerName(paymentInitiation.getPayerName())
        .reference(paymentInitiation.getReference())
        .amount(paymentInitiation.getAmount().toString())
        .build();
  }
}
