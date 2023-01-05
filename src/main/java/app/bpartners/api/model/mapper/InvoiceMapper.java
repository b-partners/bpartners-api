package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HProduct;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.PaymentInitiationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.service.InvoiceService.DRAFT_REF_PREFIX;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.FractionUtils.toAprational;
import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class InvoiceMapper {
  private final ObjectMapper objectMapper;
  private final CustomerMapper customerMapper;
  private final InvoiceJpaRepository jpaRepository;
  private final ProductMapper productMapper;
  private final AccountService accountService;
  private final PaymentInitiationService pis;

  public Invoice toDomain(
      HInvoice entity,
      List<HProduct> products) {
    List<Product> actualProducts = List.of();
    if (products != null) {
      actualProducts = products.stream()
          .map(productMapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    }
    Map<String, String> metadata = toMetadataMap(entity.getMetadataString());
    Invoice invoice = Invoice.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .fileId(entity.getFileId())
        .title(entity.getTitle())
        .comment(entity.getComment())
        .products(actualProducts)
        .sendingDate(entity.getSendingDate())
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
        .account(accountService.getAccountById(entity.getIdAccount()))
        .status(entity.getStatus())
        .toBeRelaunched(entity.isToBeRelaunched())
        .createdAt(entity.getCreatedDatetime())
        .metadata(metadata)
        .totalVat(computeTotalVat(actualProducts))
        .totalPriceWithoutVat(computeTotalPriceWithoutVat(actualProducts))
        .totalPriceWithVat(computeTotalPriceWithVat(actualProducts))
        .build();
    if (entity.getStatus().equals(CONFIRMED) || entity.getStatus().equals(PAID)) {
      PaymentRedirection paymentRedirection = pis.initiateInvoicePayment(invoice);
      invoice.setPaymentUrl(paymentRedirection.getRedirectUrl());
      invoice.setRef(invoice.getRealReference());
    } else {
      invoice.setPaymentUrl(null);
      if (invoice.getRef() != null && !invoice.getRef().isBlank()) {
        invoice.setRef(DRAFT_REF_PREFIX + invoice.getRealReference());
      }
    }
    return invoice;
  }

  public Invoice toDomain(
      HInvoice entity,
      List<HProduct> products,
      String fileId) {
    return toDomain(entity, products).toBuilder()
        .fileId(fileId)
        .build();
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
  public HInvoice toEntity(Invoice domain) {
    Optional<HInvoice> persisted = jpaRepository.findById(domain.getId());
    String fileId = persisted.isPresent() ? persisted.get().getFileId() : domain.getFileId();
    String id = domain.getId();
    if (persisted.isPresent()) {
      HInvoice persistedValue = persisted.get();
      if (persistedValue.getStatus() == InvoiceStatus.PROPOSAL
          && domain.getStatus() == InvoiceStatus.CONFIRMED) {
        id = randomUUID().toString();
        //TODO: add test for this
        persistedValue.setStatus(InvoiceStatus.PROPOSAL_CONFIRMED);
        jpaRepository.save(persistedValue);
      }
    }
    return HInvoice.builder()
        .id(id)
        .fileId(fileId)
        .comment(domain.getComment())
        .ref(domain.getRealReference())
        .title(domain.getTitle())
        .idAccount(domain.getAccount().getId())
        .sendingDate(domain.getSendingDate())
        .customer(customerMapper.toEntity(domain.getCustomer()))
        .customerEmail(domain.getCustomerEmail())
        .customerAddress(domain.getCustomerAddress())
        .customerCity(domain.getCustomerCity())
        .customerPhone(domain.getCustomerPhone())
        .customerCountry(domain.getCustomerCountry())
        .customerWebsite(domain.getCustomerWebsite())
        .customerZipCode(domain.getCustomerZipCode())
        .toPayAt(domain.getToPayAt())
        .updatedAt(Instant.now())
        .createdDatetime(getCreatedDatetime(persisted))
        .status(domain.getStatus())
        .toBeRelaunched(domain.isToBeRelaunched())
        .metadataString(objectMapper.writeValueAsString(domain.getMetadata()))
        .build();
  }

  private static Instant getCreatedDatetime(Optional<HInvoice> persisted) {
    return persisted.map(HInvoice::getCreatedDatetime).orElse(Instant.now());
  }

  private Fraction computeTotalVat(List<Product> products) {
    return computeSum(products, products.stream()
        .map(Product::getTotalVat));
  }

  private Fraction computeTotalPriceWithoutVat(List<Product> products) {
    return computeSum(products, products.stream()
        .map(Product::getTotalWithoutVat));
  }

  private Fraction computeTotalPriceWithVat(List<Product> products) {
    return computeSum(products, products.stream()
        .map(Product::getTotalPriceWithVat));
  }

  private Fraction computeSum(List<Product> products, Stream<Fraction> fractionStream) {
    if (products == null) {
      return new Fraction();
    }
    Aprational aprational = fractionStream
        .map(a -> toAprational(a.getNumerator(), a.getDenominator()))
        .reduce(new Aprational(0), Aprational::add);
    return parseFraction(aprational);
  }
}
