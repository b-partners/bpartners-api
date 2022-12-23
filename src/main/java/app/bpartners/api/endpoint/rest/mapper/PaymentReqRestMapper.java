package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.PaymentRequest;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.PaymentReqValidator;
import app.bpartners.api.repository.InvoiceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class PaymentReqRestMapper {
  private final PaymentReqValidator paymentValidator;
  private final InvoiceRestMapper invoiceRestMapper;
  private final InvoiceRepository invoiceRepository;

  public app.bpartners.api.model.PaymentInitiation toDomain(PaymentInitiation rest) {
    RedirectionStatusUrls statusUrls = rest.getRedirectionStatusUrls();
    paymentValidator.accept(rest);
    return app.bpartners.api.model.PaymentInitiation.builder()
        .id(rest.getId())
        .label(rest.getLabel())
        .reference(rest.getReference())
        .amount(parseFraction(rest.getAmount()))
        .payerEmail(rest.getPayerEmail())
        .payerName(rest.getPayerName())
        .successUrl(statusUrls.getSuccessUrl())
        .failureUrl(statusUrls.getFailureUrl())
        .build();
  }

  public PaymentRequest toRest(app.bpartners.api.model.PaymentRequest domain) {
    String invoiceId = domain.getInvoiceId();
    Invoice invoice =
        invoiceId == null ? null :
            invoiceRestMapper.toRest(invoiceRepository.getById(invoiceId));
    return new PaymentRequest()
        .id(domain.getId())
        .label(domain.getLabel())
        .invoice(invoice)
        .endToEndId(domain.getSessionId())
        .payerEmail(domain.getPayerEmail())
        .payerName(domain.getPayerName())
        .reference(domain.getReference())
        .amount(domain.getAmount().getCentsRoundUp());
  }
}
