package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class PaymentRequestMapper {
  private final PaymentRequestJpaRepository jpaRepository;
  private final AuthenticatedResourceProvider provider;

  public HPaymentRequest toEntity(
      PaymentRedirection paymentRedirection,
      app.bpartners.api.model.PaymentInitiation paymentInitiation,
      String idInvoice
  ) {
    Optional<HPaymentRequest> persisted =
        jpaRepository.findBySessionId(paymentRedirection.getMeta().getSessionId());
    if (persisted.isPresent()) {
      throw new ApiException(SERVER_EXCEPTION,
          "PaymentRequest." + paymentRedirection.getMeta().getSessionId()
              + " already exists and is linked with PaymentRequest."
              + persisted.get().getId());
    }
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

  public PaymentRequest toDomain(HPaymentRequest entity) {
    return PaymentRequest.builder()
        .id(entity.getId())
        .sessionId(entity.getSessionId())
        .invoiceId(entity.getIdInvoice())
        .accountId(entity.getAccountId())
        .label(entity.getLabel())
        .reference(entity.getReference())
        .payerName(entity.getPayerName())
        .payerEmail(entity.getPayerEmail())
        .amount(parseFraction(entity.getAmount()))
        .build();
  }
}
