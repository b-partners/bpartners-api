package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
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
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL_CONFIRMED;
import static app.bpartners.api.service.InvoiceService.DRAFT_REF_PREFIX;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.FractionUtils.toAprational;
import static java.util.UUID.randomUUID;

@Slf4j
@Component
@AllArgsConstructor
public class InvoiceMapper {
  private final ObjectMapper objectMapper;
  private final CustomerMapper customerMapper;
  private final InvoiceJpaRepository jpaRepository;
  private final AccountService accountService;
  private final PaymentInitiationService pis;
  private final InvoiceProductMapper productMapper;

  public Invoice toDomain(HInvoice entity) {
    List<InvoiceProduct> actualProducts = entity.getProducts() == null
        ? List.of() : entity.getProducts().stream()
        .map(productMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    Map<String, String> metadata = toMetadataMap(entity.getMetadataString());
    Invoice invoice = Invoice.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .fileId(entity.getFileId())
        .title(entity.getTitle())
        .comment(entity.getComment())
        .paymentUrl(entity.getPaymentUrl())
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
      invoice.setRef(invoice.getRealReference());
    } else {
      if (invoice.getRef() != null && !invoice.getRef().isBlank()) {
        invoice.setRef(DRAFT_REF_PREFIX + invoice.getRealReference());
      }
    }
    return invoice;
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
  public HInvoice toEntity(Invoice domain, boolean isToBeCrupdated) {
    String id = domain.getId();
    String fileId = domain.getFileId();
    String paymentUrl = domain.getPaymentUrl();
    Fraction totalPriceWithVat = computeTotalPriceWithVat(domain.getProducts());
    Optional<HInvoice> persisted = jpaRepository.findById(id);
    List<HInvoiceProduct> actualProducts = List.of();
    if (isToBeCrupdated && persisted.isPresent()) {
      HInvoice persistedValue = persisted.get();
      actualProducts = persistedValue.getProducts();
      fileId = persistedValue.getFileId();
      if (domain.getStatus() == CONFIRMED
          && persistedValue.getStatus() == PROPOSAL) {
        setIntermediateStatus(persistedValue);
        id = randomUUID().toString(); //Generate a new invoice
        if (totalPriceWithVat.getCentsAsDecimal() != 0) {
          paymentUrl =
              pis.initiateInvoicePayment(domain, totalPriceWithVat).getRedirectUrl();
        }
      } else if (domain.getStatus() == PAID
          && persistedValue.getStatus() == CONFIRMED) {
        paymentUrl = persistedValue.getPaymentUrl();
      }
    }
    return HInvoice.builder()
            .

        id(id)
            .

        fileId(fileId)
            .

        comment(domain.getComment())
            .

        paymentUrl(paymentUrl)
            .

        ref(domain.getRealReference())
            .

        title(domain.getTitle())
            .

        idAccount(domain.getAccount().

            getId())
            .

        sendingDate(domain.getSendingDate())
            .

        customer(customerMapper.toEntity(domain.getCustomer()))
            .

        customerEmail(domain.getCustomerEmail())
            .

        customerAddress(domain.getCustomerAddress())
            .

        customerCity(domain.getCustomerCity())
            .

        customerPhone(domain.getCustomerPhone())
            .

        customerCountry(domain.getCustomerCountry())
            .

        customerWebsite(domain.getCustomerWebsite())
            .

        customerZipCode(domain.getCustomerZipCode())
            .

        toPayAt(domain.getToPayAt())
            .

        updatedAt(Instant.now())
            .

        createdDatetime(getCreatedDatetime(persisted))
            .

        status(domain.getStatus())
            .

        toBeRelaunched(domain.isToBeRelaunched())
            .

        products(actualProducts)
            .

        metadataString(objectMapper.writeValueAsString(domain.getMetadata()))
            .

        build();

  }

  public HInvoice toEntity(Invoice domain, String fileId) {
    return toEntity(domain, true).toBuilder()
        .fileId(fileId)
        .build();
  }

  private void setIntermediateStatus(HInvoice persistedValue) {
    persistedValue.setStatus(PROPOSAL_CONFIRMED);
    jpaRepository.save(persistedValue);
  }

  private static Instant getCreatedDatetime(Optional<HInvoice> persisted) {
    return persisted.map(HInvoice::getCreatedDatetime).orElse(Instant.now());
  }

  private Fraction computeTotalVat(List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(InvoiceProduct::getTotalVat));
  }

  private Fraction computeTotalPriceWithoutVat(List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(InvoiceProduct::getTotalWithoutVat));
  }

  private Fraction computeTotalPriceWithVat(List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(InvoiceProduct::getTotalPriceWithVat));
  }

  private Fraction computeSum(List<InvoiceProduct> products, Stream<Fraction> fractionStream) {
    if (products == null) {
      return new Fraction();
    }
    Aprational aprational = fractionStream
        .map(a -> toAprational(a.getNumerator(), a.getDenominator()))
        .reduce(new Aprational(0), Aprational::add);
    return parseFraction(aprational);
  }
}
