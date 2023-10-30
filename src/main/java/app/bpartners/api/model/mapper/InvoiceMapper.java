package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.TransactionInvoiceDetails;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.service.InvoiceService.DRAFT_REF_PREFIX;
import static app.bpartners.api.service.InvoiceService.PROPOSAL_REF_PREFIX;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.FractionUtils.toAprational;
import static app.bpartners.api.service.utils.PaymentUtils.computeTotalPriceFromPaymentReqEntity;

@Component
@AllArgsConstructor
@Slf4j
public class InvoiceMapper {
  private final ObjectMapper objectMapper;
  private final CustomerMapper customerMapper;
  private final InvoiceProductMapper productMapper;
  private final PaymentRequestMapper paymentRequestMapper;

  public Invoice toDomain(HInvoice entity, User user) {
    return toDomain(entity).toBuilder()
        .user(user)
        .build();
  }

  //TODO: split to specific sub-mapper
  public Invoice toDomain(HInvoice entity) {
    if (entity == null) {
      return null;
    }
    Integer delayInPaymentAllowed = entity.getDelayInPaymentAllowed();
    String delayPenaltyPercent = entity.getDelayPenaltyPercent();
    List<InvoiceProduct> actualProducts = toInvoiceProducts(entity);
    Fraction discount = parseFraction(entity.getDiscountPercent());
    Invoice invoice = Invoice.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .fileId(entity.getFileId())
        .title(entity.getTitle())
        .comment(entity.getComment())
        .paymentType(entity.getPaymentType())
        .paymentUrl(entity.getPaymentUrl())
        .products(actualProducts)
        .sendingDate(entity.getSendingDate())
        .validityDate(entity.getValidityDate())
        .delayInPaymentAllowed(delayInPaymentAllowed)
        .delayPenaltyPercent(parseFraction(delayPenaltyPercent))
        .updatedAt(entity.getUpdatedAt())
        .toPayAt(entity.getToPayAt())
        .customer(customerMapper.toDomain(entity.getCustomer()))
        .customerEmail(entity.getCustomerEmail())
        .customerAddress(entity.getCustomerAddress())
        .customerCity(entity.getCustomerCity())
        .customerPhone(entity.getCustomerPhone())
        .customerCountry(entity.getCustomerCountry())
        .customerWebsite(entity.getCustomerWebsite())
        .customerZipCode(entity.getCustomerZipCode())
        //TODO: add user
        .status(entity.getStatus())
        .archiveStatus(entity.getArchiveStatus())
        .toBeRelaunched(entity.isToBeRelaunched())
        .createdAt(entity.getCreatedDatetime())
        .metadata(toMetadataMap(entity.getMetadataString()))
        //total without vat and without discount
        .totalPriceWithoutDiscount(computePriceWithoutDiscount(actualProducts))
        //total without vat but with discount
        .totalPriceWithoutVat(computePriceNoVatWithDiscount(discount, actualProducts))
        //total vat with discount
        .totalVat(computeTotalVatWithDiscount(discount, actualProducts))
        //total with vat and with discount
        .totalPriceWithVat(
            computeTotalPriceWithVatAndDiscount(discount, actualProducts))
        .discount(InvoiceDiscount.builder()
            .percentValue(parseFraction(entity.getDiscountPercent()))
            .amountValue(computeTotalDiscountAmount(discount, actualProducts))
            .build())
        .paymentRegulations(toCreatePaymentRegulations(entity))
        .paymentMethod(entity.getPaymentMethod())
        .build();
    return updateInvoiceReference(entity, invoice);
  }

  private static Invoice updateInvoiceReference(HInvoice entity, Invoice invoice) {
    if (entity.getStatus().equals(CONFIRMED) || entity.getStatus().equals(PAID)) {
      invoice.setRef(invoice.getRealReference());
    } else {
      if (invoice.getRef() != null && !invoice.getRef().isBlank()) {
        if (invoice.getStatus().equals(PROPOSAL)) {
          invoice.setRef(PROPOSAL_REF_PREFIX + invoice.getRealReference());
        } else {
          invoice.setRef(DRAFT_REF_PREFIX + invoice.getRealReference());
        }
      }
    }
    return invoice;
  }

  public List<CreatePaymentRegulation> toCreatePaymentRegulations(HInvoice entity) {
    List<HPaymentRequest> paymentRequests = entity.getPaymentRequests();
    Fraction totalPrice = computeTotalPriceFromPaymentReqEntity(paymentRequests);
    return paymentRequests.stream()
        .map(payment -> {
          Fraction percent = totalPrice.getCentsRoundUp() == 0 ? new Fraction()
              : parseFraction(payment.getAmount()).operate(totalPrice,
              Aprational::divide);
          return paymentRequestMapper.toPaymentRegulation(
              new PaymentRequest(payment), percent);
        })
        .toList();
  }

  public TransactionInvoiceDetails toTransactionInvoice(HInvoice entity) {
    return entity == null ? null
        : TransactionInvoiceDetails.builder()
        .idInvoice(entity.getId())
        .fileId(entity.getFileId())
        .build();
  }

  private List<InvoiceProduct> toInvoiceProducts(HInvoice entity) {
    return entity.getProducts() == null
        ? List.of() : entity.getProducts().stream()
        .map(productMapper::toDomain)
        .toList();
  }

  @SneakyThrows
  private Map<String, String> toMetadataMap(String metadataString) {
    if (metadataString == null) {
      return Map.of();
    }
    return objectMapper.readValue(metadataString, new TypeReference<>() {
    });
  }

  @SneakyThrows
  public HInvoice toEntity(
      Invoice domain,
      List<HPaymentRequest> paymentRequests,
      List<HInvoiceProduct> products) {
    return HInvoice.builder()
        .id(domain.getId())
        .fileId(domain.getFileId())
        .comment(domain.getComment())
        .paymentUrl(domain.getPaymentUrl())
        .ref(domain.getRealReference())
        .title(domain.getTitle())
        .idUser(domain.getUser().getId())
        .status(domain.getStatus())
        .archiveStatus(
            domain.getArchiveStatus() == null
                ? ArchiveStatus.ENABLED
                : domain.getArchiveStatus())
        .toBeRelaunched(domain.isToBeRelaunched())
        .customer(domain.getCustomer() == null
            ? null
            : customerMapper.toEntity(domain.getCustomer()))
        .customerEmail(domain.getCustomerEmail())
        .customerAddress(domain.getCustomerAddress())
        .customerCity(domain.getCustomerCity())
        .customerPhone(domain.getCustomerPhone())
        .customerCountry(domain.getCustomerCountry())
        .customerWebsite(domain.getCustomerWebsite())
        .customerZipCode(domain.getCustomerZipCode())
        .paymentType(domain.getPaymentType())
        .validityDate(domain.getValidityDate())
        .sendingDate(domain.getSendingDate())
        .toPayAt(domain.getToPayAt())
        .updatedAt(Instant.now())
        .createdDatetime(domain.getCreatedAt())
        .delayInPaymentAllowed(domain.getDelayInPaymentAllowed())
        .delayPenaltyPercent(domain.getDelayPenaltyPercent().toString())
        .paymentRequests(paymentRequests)
        .products(products)
        .metadataString(objectMapper.writeValueAsString(domain.getMetadata()))
        .discountPercent(domain.getDiscount().getPercentValue().toString())
        .paymentMethod(domain.getPaymentMethod())
        .build();
  }

  public static Fraction computeTotalDiscountAmount(Fraction discount,
                                                    List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(product ->
            product.getDiscountAmount(discount)));
  }

  public static Fraction computeTotalVatWithDiscount(Fraction discount,
                                                     List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(product ->
            product.getVatWithDiscount(discount)));
  }

  public static Fraction computePriceWithoutDiscount(List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(InvoiceProduct::getPriceWithoutVat));
  }

  public static Fraction computeTotalPriceWithVatAndDiscount(
      Fraction discount, List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(product ->
            product.getPriceWithVatAndDiscount(discount)));
  }

  public static Fraction computePriceNoVatWithDiscount(Fraction discount,
                                                       List<InvoiceProduct> actualProducts) {
    return computeSum(actualProducts, actualProducts.stream()
        .map(product ->
            product.getPriceNoVatWithDiscount(discount)));
  }

  private static Fraction computeSum(List<InvoiceProduct> products,
                                     Stream<Fraction> fractionStream) {
    if (products == null) {
      return new Fraction();
    }
    Aprational aprational = fractionStream
        .map(a -> toAprational(a.getNumerator(), a.getDenominator()))
        .reduce(new Aprational(0), Aprational::add);
    return parseFraction(aprational);
  }
}
