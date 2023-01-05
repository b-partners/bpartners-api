package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.ProductMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  //  private final ProductRepository productRepository;
  private final InvoiceMapper mapper;
  private final InvoiceProductJpaRepository invoiceProductRepository;
  private final ProductMapper productMapper;

  @Override
  public Invoice crupdate(Invoice toCrupdate) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findByIdAccountAndRefAndStatus(
        toCrupdate.getAccount().getId(), toCrupdate.getRealReference(), toCrupdate.getStatus());
    if (optionalInvoice.isPresent()) {
      HInvoice existingInvoice = optionalInvoice.get();
      if (!toCrupdate.getId().equals(existingInvoice.getId())) {
        throw new BadRequestException(
            "The invoice reference must be unique however the given reference ["
                + toCrupdate.getRef()
                + "] is already used by invoice." + existingInvoice.getId());
      }
    }
    HInvoice entity = jpaRepository.save(mapper.toEntity(toCrupdate));
    List<HProduct> products = getProductEntities(toCrupdate);
    entity.setInvoiceProducts(List.of(invoiceProductRepository.save(HInvoiceProduct.builder()
        .products(products)
        .build())));
    //TODO: check the utility of this
    //    if (!Objects.equals(toCrupdate.getId(), entity.getId())) {
    //      toCrupdate.setId(entity.getId());
    //      if (toCrupdate.getInvoiceCustomer() != null) {
    //        toCrupdate.getInvoiceCustomer().setId(null);
    //        toCrupdate.getInvoiceCustomer().setIdInvoice(entity.getId());
    //      }
    //      cloneProducts(toCrupdate.getAccount().getId(), toCrupdate.getId());
    //    }
    return mapper.toDomain(entity, getProductsFromInvoice(entity), entity.getFileId());
  }

  @Override
  public Invoice getById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isEmpty()) {
      throw new NotFoundException("Invoice." + invoiceId + " is not found");
    }
    HInvoice invoice = optionalInvoice.get();
    return mapper.toDomain(invoice,
        getProductsFromInvoice(invoice));
  }

  @Override
  public Optional<Invoice> getOptionalById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isPresent()) {
      HInvoice invoice = optionalInvoice.get();
      return Optional.of(mapper.toDomain(invoice, getProductsFromInvoice(invoice)));
    }
    return Optional.empty();
  }

  @Override
  public List<Invoice> findAllByAccountIdAndStatus(String accountId, InvoiceStatus status, int page,
                                                   int pageSize) {
    PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository.findAllByIdAccountAndStatus(accountId, status, pageRequest).stream()
        .map(invoice ->
            mapper.toDomain(invoice, getProductsFromInvoice(invoice)))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Invoice> findAllByAccountId(String accountId, int page, int pageSize) {
    PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository.findAllByIdAccount(accountId, pageable).stream()
        .map(invoice -> mapper.toDomain(invoice, getProductsFromInvoice(invoice)))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<HProduct> getProductEntities(Invoice toCrupdate) {
    return toCrupdate.getProducts().stream()
        .map(product -> productMapper.toEntity(toCrupdate.getAccount().getId(), product))
        .collect(Collectors.toUnmodifiableList());
  }


  private static List<HProduct> getProductsFromInvoice(HInvoice invoice) {
    return invoice.getInvoiceProducts().isEmpty()
        ? List.of() : invoice.getInvoiceProducts().get(0).getProducts();
  }

  //  private List<Product> ignoreIdsOf(List<Product> actual) {
  //    return actual.stream()
  //        .peek(product -> product.setId(null))
  //        .collect(Collectors.toUnmodifiableList());
  //  }

  //  private void cloneProducts(String accountId, String invoiceId) {
  //    productRepository.saveAll(accountId, ignoreIdsOf(productRepository.findByIdInvoice(invoiceId)));
  //  }
}
