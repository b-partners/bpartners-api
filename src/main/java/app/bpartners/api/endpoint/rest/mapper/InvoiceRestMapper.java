package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.PaymentRegulation;
import app.bpartners.api.endpoint.rest.model.PaymentRequest;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.model.TransactionInvoice;
import app.bpartners.api.endpoint.rest.validator.CrupdateInvoiceValidator;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.InvoiceService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.Invoice.DEFAULT_DELAY_PENALTY_PERCENT;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
@AllArgsConstructor
public class InvoiceRestMapper {
  private final CustomerRestMapper customerMapper;
  private final CustomerRepository customerRepository;
  private final ProductRestMapper productRestMapper;
  private final AccountService accountService;
  private final CrupdateInvoiceValidator crupdateInvoiceValidator;
  private final InvoiceService invoiceService;

  public Invoice toRest(app.bpartners.api.model.Invoice domain) {
    if (domain == null) {
      return null;
    }

    //TODO: deprecated use validityDate instead of toPayAt
    LocalDate toPayAt = domain.getToPayAt();
    if (domain.getStatus() != PAID && domain.getStatus() != CONFIRMED
        && domain.getToPayAt() == null) {
      toPayAt = domain.getValidityDate();
    }

    return new Invoice()
        .id(domain.getId())
        .fileId(domain.getFileId())
        .comment(domain.getComment())
        .ref(domain.getRef())
        .title(domain.getTitle())
        .updatedAt(domain.getUpdatedAt())
        .createdAt(domain.getCreatedAt())
        .customer(customerMapper.toRest(domain.getCustomer()))
        .status(domain.getStatus())
        .products(getProducts(domain))
        .totalVat(domain.getTotalVat().getCentsRoundUp())
        .paymentUrl(domain.getPaymentUrl())
        .totalPriceWithoutVat(domain.getTotalPriceWithoutVat().getCentsRoundUp())
        .totalPriceWithVat(domain.getTotalPriceWithVat().getCentsRoundUp())
        .sendingDate(domain.getSendingDate())
        .validityDate(domain.getValidityDate())
        .delayInPaymentAllowed(domain.getDelayInPaymentAllowed())
        .delayPenaltyPercent(domain.getDelayPenaltyPercent().getCentsRoundUp())
        .metadata(domain.getMetadata())
        .paymentRegulations(domain.getMultiplePayments().stream()
            .map(payment -> new PaymentRegulation()
                .maturityDate(payment.getMaturityDate())
                .paymentRequest(new PaymentRequest()
                    .id(payment.getEndToEndId())
                    .reference(payment.getReference())
                    .paymentUrl(payment.getPaymentUrl())
                    .label(payment.getComment())
                    .amount(payment.getAmount().getCentsRoundUp())
                    .payerName(payment.getPayerName())
                    .payerEmail(payment.getPayerEmail())
                    .paymentUrl(payment.getPaymentUrl())
                    .initiatedDatetime(payment.getInitiatedDatetime())))
            .collect(Collectors.toUnmodifiableList()))
        .toPayAt(toPayAt);
  }

  public TransactionInvoice toRest(app.bpartners.api.model.TransactionInvoice transactionInvoice) {
    return transactionInvoice == null ? null
        : new TransactionInvoice()
        .invoiceId(transactionInvoice.getInvoiceId())
        .fileId(transactionInvoice.getFileId());
  }

  public app.bpartners.api.model.Invoice toDomain(
      String accountId, String id, CrupdateInvoice rest) {
    crupdateInvoiceValidator.accept(rest);

    if (!invoiceService.hasAvailableReference(accountId, rest.getRef(), rest.getStatus())) {
      throw new BadRequestException("Invoice.reference=" + rest.getRef() + " is already used");
    }

    //TODO: deprecated use validityDate instead of toPayAt
    LocalDate validityDate = rest.getValidityDate();
    if (validityDate == null && rest.getToPayAt() != null
        && rest.getStatus() != CONFIRMED && rest.getStatus() != PAID) {
      log.warn("DEPRECATED: DRAFT and PROPOSAL invoice must use validityDate"
          + " instead of toPayAt attribute during crupdate");
      validityDate = rest.getToPayAt();
    }

    return app.bpartners.api.model.Invoice.builder()
        .id(id)
        .title(rest.getTitle())
        .ref(rest.getRef())
        .comment(rest.getComment())
        .paymentType(rest.getPaymentType())
        .multiplePayments(getMultiplePayments(rest))
        .customer(rest.getCustomer() != null
            ? customerRepository.findById(rest.getCustomer().getId()) : null)
        .sendingDate(rest.getSendingDate())
        .validityDate(validityDate)
        .delayInPaymentAllowed(rest.getDelayInPaymentAllowed())
        .delayPenaltyPercent(
            rest.getDelayPenaltyPercent() == null
                ? parseFraction(DEFAULT_DELAY_PENALTY_PERCENT)
                : parseFraction(rest.getDelayPenaltyPercent()))
        .status(rest.getStatus())
        .toPayAt(rest.getToPayAt())
        .account(accountService.getAccountById(accountId))
        .products(getProducts(rest))
        .metadata(rest.getMetadata() == null ? Map.of() : rest.getMetadata())
        .build();
  }

  public app.bpartners.api.model.CreatePaymentRegulation toDomain(
      app.bpartners.api.endpoint.rest.model.CreatePaymentRegulation invoicePayment) {
    if (invoicePayment.getAmount() != null && invoicePayment.getPercent() != null) {
      throw new NotImplementedException("Only amount or percent payment method should be chosen");
    }
    return app.bpartners.api.model.CreatePaymentRegulation.builder()
        .amount(parseFraction(invoicePayment.getAmount()))
        .percent(parseFraction(invoicePayment.getPercent()))
        .comment(invoicePayment.getComment())
        .maturityDate(invoicePayment.getMaturityDate())
        .build();
  }

  private List<app.bpartners.api.model.CreatePaymentRegulation> getMultiplePayments(
      CrupdateInvoice rest) {
    return rest.getPaymentRegulations() == null
        ? List.of() : rest.getPaymentRegulations().stream()
        .map(this::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  private List<InvoiceProduct> getProducts(CrupdateInvoice rest) {
    return rest.getProducts() == null ? List.of() : rest.getProducts().stream()
        .map(productRestMapper::toInvoiceDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  private List<Product> getProducts(app.bpartners.api.model.Invoice domain) {
    return domain.getProducts() == null
        ? List.of() : domain.getProducts().stream()
        .map(productRestMapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
