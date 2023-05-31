package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceDiscount;
import app.bpartners.api.endpoint.rest.model.PaymentRegulation;
import app.bpartners.api.endpoint.rest.model.PaymentRequest;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.model.TransactionInvoice;
import app.bpartners.api.endpoint.rest.model.UpdateInvoiceArchivedStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.ArchiveInvoiceValidator;
import app.bpartners.api.endpoint.rest.validator.CrupdateInvoiceValidator;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.TransactionInvoiceDetails;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotImplementedException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.CrupdateInvoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceNoVatWithDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceWithoutDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalPriceWithVatAndDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalVatWithDiscount;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
@AllArgsConstructor
public class InvoiceRestMapper {
  private final CustomerRestMapper customerMapper;
  private final ProductRestMapper productRestMapper;
  private final CrupdateInvoiceValidator crupdateInvoiceValidator;
  private final ArchiveInvoiceValidator archiveValidator;

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
        .archiveStatus(domain.getArchiveStatus())
        .paymentType(domain.getPaymentType())
        .products(getProducts(domain))
        .totalVat(domain.getTotalVat().getIntValue())
        .totalPriceWithVat(domain.getTotalPriceWithVat().getIntValue())
        .totalPriceWithoutDiscount(domain.getTotalPriceWithoutDiscount().getIntValue())
        .totalPriceWithoutVat(domain.getTotalPriceWithoutVat().getIntValue())
        .paymentUrl(domain.getPaymentUrl())
        .sendingDate(domain.getSendingDate())
        .validityDate(domain.getValidityDate())
        .delayInPaymentAllowed(domain.getDelayInPaymentAllowed())
        .delayPenaltyPercent(domain.getDelayPenaltyPercent().getIntValue())
        .metadata(domain.getMetadata())
        .paymentRegulations(domain.getPaymentRegulations().stream()
            .map(payment -> getPaymentRegulation(domain.getTotalPriceWithVat(), payment))
            .collect(Collectors.toUnmodifiableList()))
        .toPayAt(toPayAt)
        .globalDiscount(new InvoiceDiscount()
            .percentValue(
                domain.getDiscount().getPercent(domain.getTotalPriceWithVat()).getIntValue())
            .amountValue(
                domain.getDiscount().getAmount(domain.getTotalPriceWithVat()).getIntValue()));
  }

  public TransactionInvoice toRest(TransactionInvoiceDetails invoiceDetails) {
    return invoiceDetails == null ? null
        : new TransactionInvoice()
        .invoiceId(invoiceDetails.getIdInvoice())
        .fileId(invoiceDetails.getFileId());
  }

  public app.bpartners.api.model.Invoice toDomain(String idUser, String id, CrupdateInvoice rest) {
    crupdateInvoiceValidator.accept(rest);

    //TODO: deprecated use validityDate instead of toPayAt
    LocalDate validityDate = rest.getValidityDate();
    if (validityDate == null && rest.getToPayAt() != null
        && rest.getStatus() != CONFIRMED && rest.getStatus() != PAID) {
      log.warn("DEPRECATED: DRAFT and PROPOSAL invoice must use validityDate"
          + " instead of toPayAt attribute during crupdate");
      validityDate = rest.getToPayAt();
    }

    Integer delayInPaymentAllowed = rest.getDelayInPaymentAllowed();
    Integer delayPenaltyPercent = rest.getDelayPenaltyPercent();
    //TODO: check default value if necessary
    if (delayInPaymentAllowed == null && rest.getPaymentType() == CASH) {
      log.warn(
          "Delay in payment allowed is mandatory for CASH Payment type. 30 is given by default");
      delayInPaymentAllowed = 30;
    }

    //TODO: deprecated ! discount must be mandatory
    InvoiceDiscount discount = rest.getGlobalDiscount();
    if (rest.getGlobalDiscount() == null
        || (rest.getGlobalDiscount() != null
        && rest.getGlobalDiscount().getPercentValue() == null)) {
      discount = new InvoiceDiscount().percentValue(0);
    }

    app.bpartners.api.model.InvoiceDiscount discountDomain = getDiscount(discount);
    Fraction discountAsPercent = discountDomain.getPercentValue();
    List<InvoiceProduct> invoiceProducts = getProducts(rest);
    return app.bpartners.api.model.Invoice.builder()
        .id(id)
        .title(rest.getTitle())
        .ref(rest.getRef())
        .comment(rest.getComment())
        .paymentType(convertType(rest.getPaymentType()))
        .paymentRegulations(getMultiplePayments(rest))
        .customer(rest.getCustomer() == null
            ? null
            : customerMapper.toDomain(idUser, rest.getCustomer()))
        .sendingDate(rest.getSendingDate())
        .validityDate(validityDate)
        .delayInPaymentAllowed(delayInPaymentAllowed)
        .delayPenaltyPercent(parseFraction(delayPenaltyPercent))
        .status(rest.getStatus())
        .archiveStatus(rest.getArchiveStatus())
        .toPayAt(rest.getToPayAt())
        .user(AuthProvider.getAuthenticatedUser())
        //total without vat and without discount
        .totalPriceWithoutDiscount(computePriceWithoutDiscount(invoiceProducts))
        //total without vat but with discount
        .totalPriceWithoutVat(computePriceNoVatWithDiscount(discountAsPercent, invoiceProducts))
        //total vat with discount
        .totalVat(computeTotalVatWithDiscount(discountAsPercent, invoiceProducts))
        //total with vat and with discount
        .totalPriceWithVat(
            computeTotalPriceWithVatAndDiscount(discountAsPercent, invoiceProducts))
        .products(invoiceProducts)
        .metadata(rest.getMetadata() == null ? Map.of() : rest.getMetadata())
        .discount(discountDomain)
        .build();
  }

  public ArchiveInvoice toDomain(UpdateInvoiceArchivedStatus archivedStatus) {
    archiveValidator.accept(archivedStatus);
    return ArchiveInvoice.builder()
        .idInvoice(archivedStatus.getId())
        .status(archivedStatus.getArchiveStatus())
        .build();
  }

  private app.bpartners.api.model.CreatePaymentRegulation toDomain(
      app.bpartners.api.endpoint.rest.model.CreatePaymentRegulation invoicePayment) {
    if (invoicePayment.getAmount() != null && invoicePayment.getPercent() != null) {
      throw new NotImplementedException("Only amount or percent payment method should be chosen");
    }
    return app.bpartners.api.model.CreatePaymentRegulation.builder()
        .paymentRequest(app.bpartners.api.model.PaymentRequest.builder()
            .amount(parseFraction(invoicePayment.getAmount()))
            .build())
        .percent(parseFraction(invoicePayment.getPercent()))
        .comment(invoicePayment.getComment())
        .maturityDate(invoicePayment.getMaturityDate())
        .build();
  }

  private static PaymentRegulation getPaymentRegulation(
      Fraction totalPrice, CreatePaymentRegulation paymentRegulation) {
    app.bpartners.api.model.PaymentRequest payment = paymentRegulation.getPaymentRequest();
    return new PaymentRegulation()
        .maturityDate(paymentRegulation.getMaturityDate())
        .paymentRequest(new PaymentRequest()
            .id(payment.getId())
            .reference(payment.getReference())
            .paymentUrl(payment.getPaymentUrl())
            .label(payment.getLabel())
            .amount(payment.getAmount().getIntValue())
            .percentValue(
                totalPrice.getApproximatedValue() == 0
                    ? 0 :
                    payment.getAmount().operate(totalPrice,
                        (amount, price) -> {
                          amount = amount.divide(new Aprational(100));
                          price = price.divide(new Aprational(100));
                          return amount.divide(price).multiply(new Aprational(10000));
                        }).getIntValue())
            .payerName(payment.getPayerName())
            .payerEmail(payment.getPayerEmail())
            .paymentUrl(payment.getPaymentUrl())
            .initiatedDatetime(paymentRegulation.getInitiatedDatetime()));
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
}
