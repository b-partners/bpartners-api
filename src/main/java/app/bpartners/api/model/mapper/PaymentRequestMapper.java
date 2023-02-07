package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.fintecture.model.Session;
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
        .accountId(entity.getAccountId())
        .label(entity.getLabel())
        .reference(entity.getReference())
        .payerName(entity.getPayerName())
        .payerEmail(entity.getPayerEmail())
        .amount(parseFraction(entity.getAmount()))
        .build();
  }
}
