package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  private final InvoiceMapper mapper;

  @Transactional
  @Override
  public Invoice crupdate(Invoice toCrupdate) {
    HInvoice entity = mapper.toEntity(toCrupdate);
    return mapper.toDomain(jpaRepository.save(entity));
  }

  @Override
  public Invoice getById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isEmpty()) {
      throw new NotFoundException("Invoice." + invoiceId + " is not found");
    }
    return mapper.toDomain(optionalInvoice.get());
  }
}
