package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

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
        .sessionId(paymentRedirection.getMeta().getSessionId())
        .label(domain.getLabel())
        .payerEmail(domain.getPayerEmail())
        .payerName(domain.getPayerName())
        .reference(domain.getReference())
        .amount(domain.getAmount().toString())
        .build();
  }
}
