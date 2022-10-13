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
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceCustomer;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
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
    HInvoiceProduct invoiceProduct = ipJpaRepository.save(new HInvoiceProduct(toCrupdate.getId(),
        computeHInvoices(toCrupdate)));
    return mapper.toDomain(entity, invoiceCustomer, invoiceProduct.getProducts());
  }

  @Override
  public Invoice getById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isEmpty()) {
      throw new NotFoundException("Invoice." + invoiceId + " is not found");
    }
    HInvoice invoice = optionalInvoice.get();
    HInvoiceCustomer invoiceCustomer = invoice.getInvoiceCustomer();
    return mapper.toDomain(invoice, invoiceCustomer, List.of());
  }

  @Override
  public List<Invoice> findAllByAccountId(String accountId, int page, int pageSize) {
    return jpaRepository.findAllByIdAccount(accountId, PageRequest.of(page, pageSize)).stream()
        .map(invoice ->
            mapper.toDomain(invoice, invoice.getInvoiceCustomer(), List.of()))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<HProduct> computeHInvoices(Invoice invoice) {
    return invoice.getProducts().stream()
        .map(product -> {
          String accountId = invoice.getAccount().getId();
          return productMapper.toEntity(accountId, product);
        })
        .collect(Collectors.toUnmodifiableList());
  }
}
