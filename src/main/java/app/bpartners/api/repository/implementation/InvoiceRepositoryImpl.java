package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceCustomerMapper;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.ProductMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.repository.jpa.InvoiceCustomerJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceProductJpaRepository;
import app.bpartners.api.repository.jpa.ProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceCustomer;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HProduct;
import app.bpartners.api.service.PaymentInitiationService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.apfloat.Aprational;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.service.InvoiceService.DRAFT_REF_PREFIX;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.FractionUtils.toAprational;

@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  private final ProductJpaRepository productJpaRepository;
  private final ProductRepository productRepository;
  private final PaymentInitiationService pis;
  private final InvoiceCustomerJpaRepository customerJpaRepository;
  private final InvoiceMapper mapper;
  private final InvoiceCustomerMapper invoiceCustomerMapper;
  private final InvoiceProductJpaRepository ipJpaRepository;
  private final ProductMapper productMapper;

  @Override
  public Invoice crupdate(Invoice toCrupdate) {
    if (toCrupdate.getRef() != null) {
      Optional<HInvoice> existingInvoice = jpaRepository.findByIdAccountAndRefAndStatus(
          toCrupdate.getAccount().getId(), toCrupdate.getRef(), toCrupdate.getPreviousStatus());
      if (existingInvoice.isPresent()) {
        String persistedId = existingInvoice.get().getId();
        if (toCrupdate.getStatus().equals(DRAFT)
            && !persistedId.equals(toCrupdate.getId())) {
          throw new BadRequestException(
              "The invoice reference must unique however the given reference ["
                  + toCrupdate.getRef()
                  + "] is already used by invoice." + persistedId);
        }
      }
    }
    HInvoice entity = jpaRepository.save(mapper.toEntity(toCrupdate));
    boolean entityHasBeenCloned =
        !Objects.equals(toCrupdate.getId(), entity.getId());
    if (entityHasBeenCloned) {
      toCrupdate.setId(entity.getId());
      if (toCrupdate.getInvoiceCustomer() != null) {
        toCrupdate.getInvoiceCustomer().setId(null);
        toCrupdate.getInvoiceCustomer().setIdInvoice(entity.getId());
      }
      cloneProducts(toCrupdate.getAccount().getId(), toCrupdate.getId());
    }
    HInvoiceCustomer invoiceCustomer =
        invoiceCustomerMapper.toEntity(toCrupdate.getInvoiceCustomer());
    if (invoiceCustomer != null) {
      invoiceCustomer = customerJpaRepository.save(invoiceCustomer);
    }
    List<HProduct> createdProducts = null;
    if (!toCrupdate.getProducts().isEmpty()) {
      HInvoiceProduct invoiceProduct =
          ipJpaRepository.save(HInvoiceProduct.builder()
              .idInvoice(toCrupdate.getId())
              .build());
      createdProducts = productJpaRepository.saveAll(computeHInvoices(invoiceProduct, toCrupdate));
      invoiceProduct.setProducts(createdProducts);
      ipJpaRepository.save(invoiceProduct);
    }
    return refreshValues(mapper.toDomain(entity, invoiceCustomer, createdProducts));
  }

  @Override
  public Invoice getById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isEmpty()) {
      throw new NotFoundException("Invoice." + invoiceId + " is not found");
    }
    HInvoice invoice = optionalInvoice.get();
    HInvoiceCustomer invoiceCustomer = customerJpaRepository
        .findTopByIdInvoiceOrderByCreatedDatetimeDesc(invoice.getId());
    return refreshValues(mapper.toDomain(invoice, invoiceCustomer, List.of()));
  }

  @Override
  public Optional<Invoice> getOptionalById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isPresent()) {
      HInvoice invoice = optionalInvoice.get();
      HInvoiceCustomer invoiceCustomer = customerJpaRepository
          .findTopByIdInvoiceOrderByCreatedDatetimeDesc(invoice.getId());
      return Optional.of(refreshValues(mapper.toDomain(invoice, invoiceCustomer, List.of())));
    }
    return Optional.empty();
  }

  @Override
  public List<Invoice> findAllByAccountIdAndStatus(String accountId, InvoiceStatus status, int page,
                                                   int pageSize) {
    return jpaRepository.findAllByIdAccountAndStatusOrderByCreatedDatetimeDesc(
            accountId, status, PageRequest.of(page, pageSize)).stream()
        .map(invoice -> {
          HInvoiceCustomer invoiceCustomer = customerJpaRepository
              .findTopByIdInvoiceOrderByCreatedDatetimeDesc(invoice.getId());
          return refreshValues(mapper.toDomain(invoice, invoiceCustomer, List.of()));
        })
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Invoice> findAllByAccountId(String accountId, int page, int pageSize) {
    return jpaRepository.findAllByIdAccountOrderByCreatedDatetimeDesc(
            accountId, PageRequest.of(page,
                pageSize)).stream()
        .map(invoice -> {
          HInvoiceCustomer invoiceCustomer = customerJpaRepository
              .findTopByIdInvoiceOrderByCreatedDatetimeDesc(invoice.getId());
          return refreshValues(mapper.toDomain(invoice, invoiceCustomer, List.of()));
        })
        .collect(Collectors.toUnmodifiableList());
  }

  private List<HProduct> computeHInvoices(HInvoiceProduct invoiceProduct, Invoice invoice) {
    return invoice.getProducts().stream()
        .map(product -> {
          String accountId = invoice.getAccount().getId();
          return productMapper.toEntity(accountId, product, invoiceProduct);
        })
        .collect(Collectors.toUnmodifiableList());
  }

  private Invoice refreshValues(Invoice invoice) {
    List<Product> products = invoice.getProducts();
    if (products.isEmpty()) {
      products =
          productRepository.findByIdInvoice(invoice.getId());
    }
    Invoice initializedInvoice = Invoice.builder()
        .id(invoice.getId())
        .fileId(invoice.getFileId())
        .comment(invoice.getComment())
        .updatedAt(invoice.getUpdatedAt())
        .title(invoice.getTitle())
        .invoiceCustomer(invoice.getInvoiceCustomer())
        .account(invoice.getAccount())
        .status(invoice.getStatus())
        .totalVat(computeTotalVat(products))
        .totalPriceWithoutVat(computeTotalPriceWithoutVat(products))
        .totalPriceWithVat(computeTotalPriceWithVat(products))
        .products(products)
        .toPayAt(invoice.getToPayAt())
        .sendingDate(invoice.getSendingDate())
        .build();
    if (invoice.getStatus().equals(CONFIRMED) || invoice.getStatus().equals(PAID)) {
      PaymentRedirection paymentRedirection = pis.initiateInvoicePayment(initializedInvoice);
      initializedInvoice.setPaymentUrl(paymentRedirection.getRedirectUrl());
      initializedInvoice.setRef(invoice.getRealReference());
    } else {
      initializedInvoice.setPaymentUrl(null);
      if (invoice.getRef() != null && !invoice.getRef().isBlank()) {
        initializedInvoice.setRef(DRAFT_REF_PREFIX + invoice.getRealReference());
      }
    }
    return initializedInvoice;
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

  private List<Product> ignoreIdsOf(List<Product> actual) {
    return actual.stream().map(product -> {
      product.setId(null);
      return product;
    }).collect(Collectors.toUnmodifiableList());
  }

  private List<Product> cloneProducts(String accountId, String invoiceId) {
    return productRepository.saveAll(
        accountId,
        ignoreIdsOf(productRepository.findByIdInvoice(invoiceId))
    );
  }
}
