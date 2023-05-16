package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
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
import app.bpartners.api.service.PaymentInitiationService;
import app.bpartners.api.service.utils.InvoiceUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL_CONFIRMED;
import static app.bpartners.api.service.InvoiceService.DRAFT_REF_PREFIX;
import static app.bpartners.api.service.InvoiceService.PROPOSAL_REF_PREFIX;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.InvoiceUtils.computePriceNoVatWithDiscount;
import static app.bpartners.api.service.utils.InvoiceUtils.computePriceWithoutDiscount;
import static app.bpartners.api.service.utils.InvoiceUtils.computeTotalDiscountAmount;
import static app.bpartners.api.service.utils.InvoiceUtils.computeTotalPriceWithVatAndDiscount;
import static app.bpartners.api.service.utils.InvoiceUtils.computeTotalVatWithDiscount;
import static app.bpartners.api.service.utils.InvoiceUtils.getCreatedDatetime;
import static app.bpartners.api.service.utils.InvoiceUtils.getPaymentRequests;
import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
@Slf4j
public class InvoiceMapper {
  private final ObjectMapper objectMapper;
  private final CustomerMapper customerMapper;
  private final InvoiceProductMapper productMapper;
  private final InvoiceJpaRepository jpaRepository;
  private final InvoiceUtils invoiceUtils;
  private final PaymentInitiationService pis;


  //TODO: split to specific sub-mapper
  public Invoice toDomain(HInvoice entity) {
    if (entity == null) {
      return null;
    }
    Integer delayInPaymentAllowed = entity.getDelayInPaymentAllowed();
    String delayPenaltyPercent = entity.getDelayPenaltyPercent();
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
        .accountId(entity.getIdAccount())
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

  public TransactionInvoice toTransactionInvoice(HInvoice entity) {
    return entity == null ? null
        : TransactionInvoice.builder()
        .invoiceId(entity.getId())
        .fileId(entity.getFileId())
        .build();
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
  //TODO: synchronized? Isn't there an other approche?
  @SneakyThrows
  @Transactional
  public synchronized HInvoice toEntity(Invoice domain, boolean isToBeCrupdated) {
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
    List<PaymentInitiation> paymentInitiations = invoiceUtils.getPaymentInitiations(domain,
        totalPriceWithVat);
    List<HPaymentRequest> paymentRequests =
        pis.retrievePaymentEntities(paymentInitiations, id, domain.getStatus());

    synchronized (this) {
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
          Integer delayInPaymentAllowed = domain.getDelayInPaymentAllowed();
          if (delayInPaymentAllowed == null) {
            log.warn(
                "Delay in payment allowed is mandatory to retrieve invoice payment date limit."
                    + " 30 days are given by default");
            delayInPaymentAllowed = 30;
          }
          toPayAt = sendingDate.plusDays(delayInPaymentAllowed);
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
          .idAccount(domain.getAccountId())
          .status(domain.getStatus())
          .archiveStatus(
              domain.getArchiveStatus() == null ? ArchiveStatus.ENABLED : domain.getArchiveStatus())
          .toBeRelaunched(domain.isToBeRelaunched())
          .customer(domain.getCustomer() == null ? null
              : customerMapper.toEntity(domain.getCustomer()))
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
          .paymentRequests(getPaymentRequests(paymentRequests, domain))
          .products(actualProducts)
          .metadataString(objectMapper.writeValueAsString(domain.getMetadata()))
          .discountPercent(domain.getDiscount().getPercentValue().toString())
          .build();
    }
  }

  private static Fraction computeMultiplePaymentsAmount(
      List<HPaymentRequest> payments) {
    AtomicReference<Fraction> fraction = new AtomicReference<>(new Fraction());
    payments.forEach(
        payment -> fraction.set(fraction.get()
            .operate(parseFraction(payment.getAmount()), Aprational::add)));
    return fraction.get();
  }

  private String getPaymentUrl(Invoice domain, String paymentUrl, Fraction totalPriceWithVat) {
    return totalPriceWithVat.getCentsAsDecimal() != 0
        ? pis.initiateInvoicePayment(domain, totalPriceWithVat).getRedirectUrl()
        : paymentUrl;
  }
}
