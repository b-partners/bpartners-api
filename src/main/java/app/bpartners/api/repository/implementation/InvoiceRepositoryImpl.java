package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceCustomerMapper;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.ProductMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceCustomerJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceProductJpaRepository;
import app.bpartners.api.repository.jpa.ProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceCustomer;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  private final ProductJpaRepository productJpaRepository;
  private final InvoiceCustomerJpaRepository customerJpaRepository;
  private final InvoiceMapper mapper;
  private final InvoiceCustomerMapper invoiceCustomerMapper;
  private final InvoiceProductJpaRepository ipJpaRepository;
  private final ProductMapper productMapper;

  @Transactional
  @Override
  public Invoice crupdate(Invoice toCrupdate) {
    HInvoice entity = jpaRepository.save(mapper.toEntity(toCrupdate));
    HInvoiceCustomer invoiceCustomer =
        customerJpaRepository.save(
            invoiceCustomerMapper.toEntity(toCrupdate.getInvoiceCustomer()));
    HInvoiceProduct invoiceProduct =
        ipJpaRepository.save(HInvoiceProduct.builder()
            .idInvoice(toCrupdate.getId())
            .build());
    List<HProduct> createdProducts = productJpaRepository.saveAll(computeHInvoices(invoiceProduct,
        toCrupdate));
    invoiceProduct.setProducts(createdProducts);
    ipJpaRepository.save(invoiceProduct);
    return mapper.toDomain(entity, invoiceCustomer, createdProducts);
  }

  @Override
  public Invoice getById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isEmpty()) {
      throw new NotFoundException("Invoice." + invoiceId + " is not found");
    }
    HInvoice invoice = optionalInvoice.get();
    HInvoiceCustomer invoiceCustomer = customerJpaRepository
        .findTopByInvoice_IdOrderByCreatedDatetimeDesc(invoice.getId());
    return mapper.toDomain(invoice, invoiceCustomer, List.of());
  }

  @Override
  public Optional<Invoice> getOptionalById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isPresent()) {
      HInvoice invoice = optionalInvoice.get();
      HInvoiceCustomer invoiceCustomer = customerJpaRepository
          .findTopByInvoice_IdOrderByCreatedDatetimeDesc(invoice.getId());
      return Optional.of(mapper.toDomain(invoice, invoiceCustomer, List.of()));
    }
    return Optional.empty();
  }

  @Override
  public List<Invoice> findAllByAccountId(String accountId, int page, int pageSize) {
    log.info("Count:" + jpaRepository.findHInvoicesByIdAccount(accountId,
        PageRequest.of(page,
            pageSize)).size());
    return jpaRepository.findHInvoicesByIdAccount(accountId, PageRequest.of(page,
            pageSize)).stream()
        .map(invoice -> {
          HInvoiceCustomer invoiceCustomer = customerJpaRepository
              .findTopByInvoice_IdOrderByCreatedDatetimeDesc(invoice.getId());
          return mapper.toDomain(invoice, invoiceCustomer, List.of());
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
}
