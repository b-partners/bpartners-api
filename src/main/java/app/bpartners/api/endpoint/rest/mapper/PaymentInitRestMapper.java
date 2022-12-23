package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.PaymentRedirection;
import app.bpartners.api.endpoint.rest.model.PaymentRequest;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.PaymentInitValidator;
import app.bpartners.api.repository.InvoiceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class PaymentInitRestMapper {
  private final PaymentInitValidator paymentValidator;
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
        .transferState(domain.getTransferState())
        .paymentUrl(domain.getPaymentUrl())
        .status(domain.getStatus())
        .paymentScheme(domain.getPaymentScheme())
        .label(domain.getLabel())
        .invoice(invoice)
        .endToEndId(domain.getSessionId())
        .payerEmail(domain.getPayerEmail())
        .payerName(domain.getPayerName())
        .reference(domain.getReference())
        .amount(domain.getAmount().getCentsRoundUp());
  }

  public PaymentRedirection toRest(app.bpartners.api.model.PaymentRedirection domain) {
    RedirectionStatusUrls statusUrls = new RedirectionStatusUrls()
        .successUrl(domain.getSuccessUrl())
        .failureUrl(domain.getFailureUrl());
    return new PaymentRedirection()
        .id(domain.getId())
        .redirectionUrl(domain.getRedirectUrl())
        .redirectionStatusUrls(statusUrls);
  }
}
