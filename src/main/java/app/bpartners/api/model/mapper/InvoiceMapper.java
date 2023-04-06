package app.bpartners.api.model.mapper;

import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.TransactionInvoice;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.PaymentInitiationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL_CONFIRMED;
import static app.bpartners.api.service.InvoiceService.DRAFT_REF_PREFIX;
import static app.bpartners.api.service.InvoiceService.PROPOSAL_REF_PREFIX;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.FractionUtils.toAprational;
import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class InvoiceMapper {
  private final ObjectMapper objectMapper;
  private final CustomerMapper customerMapper;
  private final InvoiceJpaRepository jpaRepository;
  private final AccountService accountService;
  private final PaymentInitiationService pis;
  private final InvoiceProductMapper productMapper;
  private final PaymentRequestMapper requestMapper;

  private static Instant getCreatedDatetime(Optional<HInvoice> persisted) {
    return persisted.map(HInvoice::getCreatedDatetime).orElse(Instant.now());
  }

  //TODO: split to specific sub-mapper
  public Invoice toDomain(HInvoice entity) {
    if (entity == null) {
      return null;
    }
    Integer delayInPaymentAllowed = entity.getDelayInPaymentAllowed();
    String delayPenaltyPercent = entity.getDelayPenaltyPercent();
    if (delayInPaymentAllowed == null || delayPenaltyPercent == null
        || delayPenaltyPercent.equals("0/1")) {
      delayInPaymentAllowed = null;
      delayPenaltyPercent = "0/1";
    }
    List<InvoiceProduct> actualProducts = getActualProducts(entity);
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
        .account(accountService.getAccountById(entity.getIdAccount()))
        .status(entity.getStatus())
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
        .multiplePayments(getMultiplePayments(entity))
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

  public List<CreatePaymentRegulation> getMultiplePayments(HInvoice entity) {
    List<HPaymentRequest> paymentRequests = entity.getPaymentRequests();
    Fraction totalPrice = computeMultiplePaymentsAmount(paymentRequests);
    return paymentRequests.stream()
        .map(payment -> CreatePaymentRegulation.builder()
            .endToEndId(payment.getId())
            .percent(totalPrice.getCentsRoundUp() == 0 ? new Fraction()
                : parseFraction(payment.getAmount()).operate(totalPrice,
                Aprational::divide))
            .label(payment.getLabel())
            .comment(payment.getComment())
            .amount(parseFraction(payment.getAmount()))
            .paymentUrl(payment.getPaymentUrl())
            .reference(payment.getReference())
            .payerName(payment.getPayerName())
            .payerEmail(payment.getPayerEmail())
            .maturityDate(payment.getPaymentDueDate())
            .initiatedDatetime(payment.getCreatedDatetime())
            .build())
        .collect(Collectors.toUnmodifiableList());
  }

  private Fraction computePriceNoVatWithDiscount(Fraction discount,
                                                 List<InvoiceProduct> actualProducts) {
    return computeSum(actualProducts, actualProducts.stream()
        .map(product ->
            product.getPriceNoVatWithDiscount(discount)));
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

  //TODO: move to appropriate layer the actions
  //TODO: split to specific sub-mapper
  @SneakyThrows
  public HInvoice toEntity(Invoice domain, boolean isToBeCrupdated) {
    String id = domain.getId();
    String fileId = domain.getFileId();
    String paymentUrl = domain.getPaymentUrl();
    LocalDate sendingDate = domain.getSendingDate();
    LocalDate toPayAt = null;
    LocalDate validityDate = domain.getValidityDate();
    List<HInvoiceProduct> actualProducts = List.of();
    Fraction totalPriceWithVat =
        computeTotalPriceWithVatAndDiscount(domain.getDiscount().getPercentValue(),
            domain.getProducts());
    List<PaymentInitiation> paymentInitiations = getPaymentInitiations(domain, totalPriceWithVat);
    List<HPaymentRequest> paymentRequests =
        pis.retrievePaymentEntities(paymentInitiations, id, domain.getStatus());

    //TODO: split this into specific layer - BEGIN
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(id);
    if (isToBeCrupdated && optionalInvoice.isPresent()) {
      HInvoice entity = optionalInvoice.get();
      actualProducts = entity.getProducts();
      fileId = entity.getFileId();

      //TODO: change when we can create a confirmed from scratch
      if (domain.getStatus() == CONFIRMED && entity.getStatus() == PROPOSAL) {
        id = String.valueOf(randomUUID());
        sendingDate = LocalDate.now();
        validityDate = null;
        fileId = null;

        jpaRepository.save(entity.status(PROPOSAL_CONFIRMED));
      } else if (domain.getStatus() == PAID && entity.getStatus() == CONFIRMED) {
        validityDate = null;
        sendingDate = entity.getSendingDate();

        if (domain.getPaymentType() == CASH) {
          paymentUrl = entity.getPaymentUrl();
          toPayAt = sendingDate.plusDays(domain.getDelayInPaymentAllowed());
        } else {
          paymentRequests = entity.getPaymentRequests();
        }
      }
    }

    if (domain.getStatus() == CONFIRMED) {
      if (domain.getPaymentType() == CASH) {
        toPayAt = sendingDate.plusDays(domain.getDelayInPaymentAllowed());
        paymentUrl =
            getPaymentUrl(domain, paymentUrl, computeTotalPriceWithVatAndDiscount(
                domain.getDiscount().getPercentValue(), domain.getProducts()));
      } else {
        //TODO: check if amount changed or paymentRegulations changed and retrieve new else get
        // persisted
        paymentRequests = pis.retrievePaymentEntitiesWithUrl(
            paymentInitiations, id);
        paymentUrl = null;
      }
    }
    //TODO: split this into specific layer - END
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
        .paymentType(domain.getPaymentType())
        .validityDate(validityDate)
        .sendingDate(sendingDate)
        .toPayAt(toPayAt)
        .updatedAt(Instant.now())
        .createdDatetime(getCreatedDatetime(optionalInvoice))
        .delayInPaymentAllowed(domain.getDelayInPaymentAllowed())
        .delayPenaltyPercent(domain.getDelayPenaltyPercent().toString())
        .paymentRequests(paymentRequests)
        .products(actualProducts)
        .metadataString(objectMapper.writeValueAsString(domain.getMetadata()))
        .discountPercent(domain.getDiscount().getPercentValue().toString())
        .build();
  }

  private List<PaymentInitiation> getPaymentInitiations(
      Invoice domain,
      Fraction totalPriceWithVat) {
    List<PaymentInitiation> payments = domain.getMultiplePayments().stream()
        .map(payment -> {
          String randomId = String.valueOf(randomUUID());
          payment.setEndToEndId(randomId);
          return requestMapper.convertFromInvoice(
              randomId, domain, totalPriceWithVat, payment);
        })
        .sorted(Comparator.comparing(PaymentInitiation::getPaymentDueDate))
        .collect(Collectors.toUnmodifiableList());
    for (int i = 0; i < payments.size(); i++) {
      if (i != payments.size() - 1) {
        payments.get(i).setLabel(domain.getTitle() + " - Acompte N°" + (i + 1));
      } else {
        payments.get(i).setLabel(domain.getTitle() + " - Restant dû");
      }
    }
    return payments;
  }

  private static Fraction computeMultiplePaymentsAmount(
      List<HPaymentRequest> payments) {
    AtomicReference<Fraction> fraction = new AtomicReference<>(new Fraction());
    payments.forEach(
        payment -> fraction.set(fraction.get()
            .operate(parseFraction(payment.getAmount()), Aprational::add)));
    return fraction.get();
  }

  private Fraction computeTotalDiscountAmount(Fraction discount, List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(product ->
            product.getDiscountAmount(discount)));
  }

  private Fraction computeTotalVatWithDiscount(Fraction discount, List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(product ->
            product.getVatWithDiscount(discount)));
  }

  private Fraction computePriceWithoutDiscount(List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(InvoiceProduct::getPriceWithoutVat));
  }

  private Fraction computeTotalPriceWithVatAndDiscount(
      Fraction discount, List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(product ->
            product.getPriceWithVatAndDiscount(discount)));
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
