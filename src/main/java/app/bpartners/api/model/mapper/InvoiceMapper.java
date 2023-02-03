package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.TransactionInvoice;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.PaymentInitiationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
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
import static app.bpartners.api.model.Invoice.DEFAULT_DELAY_PENALTY_PERCENT;
import static app.bpartners.api.service.InvoiceService.DRAFT_REF_PREFIX;
import static app.bpartners.api.service.InvoiceService.PROPOSAL_REF_PREFIX;
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

  private static Instant getCreatedDatetime(Optional<HInvoice> persisted) {
    return persisted.map(HInvoice::getCreatedDatetime).orElse(Instant.now());
  }

  public Invoice toDomain(HInvoice entity) {
    if (entity == null) {
      return null;
    }
    List<InvoiceProduct> actualProducts = getActualProducts(entity);
    Invoice invoice = Invoice.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .fileId(entity.getFileId())
        .title(entity.getTitle())
        .comment(entity.getComment())
        .paymentUrl(entity.getPaymentUrl())
        .products(actualProducts)
        .sendingDate(entity.getSendingDate())
        .validityDate(entity.getValidityDate())
        .delayInPaymentAllowed(entity.getDelayInPaymentAllowed())
        .delayPenaltyPercent(entity.getDelayPenaltyPercent() == null
            ? parseFraction(DEFAULT_DELAY_PENALTY_PERCENT)
            : parseFraction(entity.getDelayPenaltyPercent()))
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
        .metadata(toMetadataMap(entity.getMetadataString()))
        .totalVat(computeTotalVat(actualProducts))
        .totalPriceWithoutVat(computeTotalPriceWithoutVat(actualProducts))
        .totalPriceWithVat(computeTotalPriceWithVat(actualProducts))
        .build();
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

  public TransactionInvoice toTransactionInvoice(HInvoice entity) {
    return entity == null ? null
        : TransactionInvoice.builder()
        .invoiceId(entity.getId())
        .fileId(entity.getFileId())
        .build();
  }

  private List<InvoiceProduct> getActualProducts(HInvoice entity) {
    return entity.getProducts() == null
        ? List.of() : entity.getProducts().stream()
        .map(productMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @SneakyThrows
  private Map<String, String> toMetadataMap(String metadataString) {
    if (metadataString == null) {
      return Map.of();
    }
    return objectMapper.readValue(metadataString, new TypeReference<>() {
    });
  }

  public HInvoice toEntity(TransactionInvoice transactionInvoice) {
    return transactionInvoice == null || transactionInvoice.getInvoiceId() == null ? null
        : jpaRepository.findById(transactionInvoice.getInvoiceId())
        .orElseThrow(
            () -> new NotFoundException(
                "Invoice." + transactionInvoice.getInvoiceId() + " is not found")
        );
  }

  @SneakyThrows
  public HInvoice toEntity(Invoice domain, boolean isToBeCrupdated) {
    String id = domain.getId();
    String fileId = domain.getFileId();
    String paymentUrl = domain.getPaymentUrl();
    LocalDate sendingDate = domain.getSendingDate();
    LocalDate toPayAt = null;
    LocalDate validityDate = domain.getValidityDate();
    List<HInvoiceProduct> actualProducts = List.of();

    Optional<HInvoice> optionalInvoice = jpaRepository.findById(id);
    if (isToBeCrupdated && optionalInvoice.isPresent()) {
      HInvoice entity = optionalInvoice.get();
      actualProducts = entity.getProducts();
      fileId = entity.getFileId();
      //TODO: change when we can create a confirmed from scratch
      if (domain.getStatus() == CONFIRMED && entity.getStatus() == PROPOSAL) {
        id = String.valueOf(randomUUID());
        sendingDate = LocalDate.now();
        toPayAt = sendingDate.plusDays(domain.getDelayInPaymentAllowed());
        validityDate = null;
        paymentUrl =
            getPaymentUrl(domain, paymentUrl, computeTotalPriceWithVat(domain.getProducts()));

        jpaRepository.save(entity.status(PROPOSAL_CONFIRMED));
      } else if (domain.getStatus() == PAID && entity.getStatus() == CONFIRMED) {
        validityDate = null;
        sendingDate = entity.getSendingDate();
        paymentUrl = entity.getPaymentUrl();
        toPayAt = sendingDate.plusDays(domain.getDelayInPaymentAllowed());
      }
    }
    return HInvoice.builder()
        .id(id)
        .fileId(fileId)
        .comment(domain.getComment())
        .paymentUrl(paymentUrl)
        .ref(domain.getRealReference())
        .title(domain.getTitle())
        .idAccount(domain.getAccount().getId())
        .status(domain.getStatus())
        .toBeRelaunched(domain.isToBeRelaunched())
        .customer(customerMapper.toEntity(domain.getCustomer()))
        .customerEmail(domain.getCustomerEmail())
        .customerAddress(domain.getCustomerAddress())
        .customerCity(domain.getCustomerCity())
        .customerPhone(domain.getCustomerPhone())
        .customerCountry(domain.getCustomerCountry())
        .customerWebsite(domain.getCustomerWebsite())
        .customerZipCode(domain.getCustomerZipCode())
        .validityDate(validityDate)
        .sendingDate(sendingDate)
        .toPayAt(toPayAt)
        .updatedAt(Instant.now())
        .createdDatetime(getCreatedDatetime(optionalInvoice))
        .delayInPaymentAllowed(domain.getDelayInPaymentAllowed())
        .delayPenaltyPercent(domain.getDelayPenaltyPercent().toString())
        .products(actualProducts)
        .metadataString(objectMapper.writeValueAsString(domain.getMetadata()))
        .build();
  }

  public HInvoice toEntity(Invoice domain, String fileId) {
    return toEntity(domain, true).toBuilder()
        .fileId(fileId)
        .build();
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

  private String getPaymentUrl(Invoice domain, String paymentUrl, Fraction totalPriceWithVat) {
    return totalPriceWithVat.getCentsAsDecimal() != 0
        ? pis.initiateInvoicePayment(domain, totalPriceWithVat).getRedirectUrl()
        : paymentUrl;
  }
}
