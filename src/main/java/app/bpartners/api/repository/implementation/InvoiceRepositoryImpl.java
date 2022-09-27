package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceCustomerMapper;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceCustomerJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceCustomer;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  private final InvoiceCustomerJpaRepository customerJpaRepository;
  private final InvoiceMapper mapper;
  private final InvoiceCustomerMapper invoiceCustomerMapper;

  @Transactional
  @Override
  public Invoice crupdate(Invoice toCrupdate) {
    HInvoice entity = jpaRepository.save(mapper.toEntity(toCrupdate));
    HInvoiceCustomer createdInvoiceCustomer =
        customerJpaRepository.save(
            invoiceCustomerMapper.toEntity(toCrupdate.getInvoiceCustomer()));
    return mapper.toDomain(entity, createdInvoiceCustomer);
  }

  @Override
  public Invoice getById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isEmpty()) {
      throw new NotFoundException("Invoice." + invoiceId + " is not found");
    }
    HInvoice invoice = optionalInvoice.get();
    HInvoiceCustomer invoiceCustomer = invoice.getInvoiceCustomer();
    return mapper.toDomain(invoice, invoiceCustomer);
  }
}
