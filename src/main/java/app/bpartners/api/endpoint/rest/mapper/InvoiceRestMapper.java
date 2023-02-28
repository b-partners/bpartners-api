package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceDiscount;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.PaymentRegulation;
import app.bpartners.api.endpoint.rest.model.PaymentRequest;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.model.TransactionInvoice;
import app.bpartners.api.endpoint.rest.validator.CrupdateInvoiceValidator;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.service.AccountService;
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
  private final InvoiceJpaRepository invoiceJpaRepository;

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
        .paymentType(domain.getPaymentType())
        .products(getProducts(domain))
        .totalVat(domain.getTotalVat().getCentsRoundUp())
        .paymentUrl(domain.getPaymentUrl())
        .totalPriceWithoutDiscount(domain.getTotalPriceWithoutDiscount().getCentsRoundUp())
        .totalPriceWithoutVat(domain.getTotalPriceWithoutVat().getCentsRoundUp())
        .totalPriceWithVat(domain.getTotalPriceWithVat().getCentsRoundUp())
        .sendingDate(domain.getSendingDate())
        .validityDate(domain.getValidityDate())
        .delayInPaymentAllowed(domain.getDelayInPaymentAllowed())
        .delayPenaltyPercent(domain.getDelayPenaltyPercent().getCentsRoundUp())
        .metadata(domain.getMetadata())
        .paymentRegulations(domain.getMultiplePayments().stream()
            .map(InvoiceRestMapper::getPaymentRegulation)
            .collect(Collectors.toUnmodifiableList()))
        .toPayAt(toPayAt)
        .globalDiscount(new InvoiceDiscount()
            .percentValue(
                domain.getDiscount().getPercent(domain.getTotalPriceWithVat()).getCentsRoundUp())
            .amountValue(
                domain.getDiscount().getAmount(domain.getTotalPriceWithVat()).getCentsRoundUp()));
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

    if (!hasAvailableReference(accountId, id, rest.getRef(), rest.getStatus())) {
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

    //TODO: deprecated ! discount must be mandatory
    InvoiceDiscount discount = rest.getGlobalDiscount();
    if (rest.getGlobalDiscount() == null
        || (rest.getGlobalDiscount() != null
        && rest.getGlobalDiscount().getPercentValue() == null)) {
      discount = new InvoiceDiscount().percentValue(0);
    }

    return app.bpartners.api.model.Invoice.builder()
        .id(id)
        .title(rest.getTitle())
        .ref(rest.getRef())
        .comment(rest.getComment())
        .paymentType(convertType(rest.getPaymentType()))
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
        .discount(getDiscount(discount))
        .build();
  }

  private app.bpartners.api.model.CreatePaymentRegulation toDomain(
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

  private static PaymentRegulation getPaymentRegulation(CreatePaymentRegulation payment) {
    return new PaymentRegulation()
        .maturityDate(payment.getMaturityDate())
        .paymentRequest(new PaymentRequest()
            .id(payment.getEndToEndId())
            .reference(payment.getReference())
            .paymentUrl(payment.getPaymentUrl())
            .label(payment.getComment())
            .amount(payment.getAmount().getCentsRoundUp())
            .payerName(payment.getPayerName())
            .payerEmail(payment.getPayerEmail())
            .initiatedDatetime(payment.getInitiatedDatetime()));
  }

  private app.bpartners.api.model.InvoiceDiscount getDiscount(
      app.bpartners.api.endpoint.rest.model.InvoiceDiscount discount) {
    return app.bpartners.api.model.InvoiceDiscount.builder()
        .percentValue(parseFraction(discount.getPercentValue()))
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

  private Invoice.PaymentTypeEnum convertType(
      CrupdateInvoice.PaymentTypeEnum crupdateInvoiceType) {
    if (crupdateInvoiceType == null) {
      return null;
    }
    switch (crupdateInvoiceType.getValue()) {
      case "CASH":
        return Invoice.PaymentTypeEnum.CASH;
      case "IN_INSTALMENT":
        return Invoice.PaymentTypeEnum.IN_INSTALMENT;
      default:
        throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION,
            "Payment type " + crupdateInvoiceType.getValue() + " not found");
    }
  }

  private boolean hasAvailableReference(String accountId, String invoiceId, String reference,
                                        InvoiceStatus status) {
    if (reference == null) {
      return true;
    }
    List<HInvoice> actual =
        invoiceJpaRepository.findByIdAccountAndRefAndStatus(accountId, reference, status);
    return actual.isEmpty() || actual.get(0).getId().equals(invoiceId);
  }
}
