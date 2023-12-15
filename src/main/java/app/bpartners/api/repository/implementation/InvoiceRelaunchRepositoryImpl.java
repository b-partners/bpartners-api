package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.RelaunchType;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceRelaunchMapper;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceRelaunchJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class InvoiceRelaunchRepositoryImpl implements InvoiceRelaunchRepository {
  private final InvoiceRelaunchJpaRepository jpaRepository;
  private final InvoiceRelaunchMapper mapper;
  private final InvoiceRepository invoiceRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;

  @Override
  public List<InvoiceRelaunch> getByInvoiceId(String invoiceId, String type, Pageable pageable) {
    if (type == null) {
      return jpaRepository.getByInvoiceId(invoiceId, pageable).stream()
          .map(
              invoiceRelaunch ->
                  mapper.toDomain(invoiceRelaunch, invoiceRepository.getById(invoiceId)))
          .toList();
    } else {
      RelaunchType enumType;
      try {
        enumType = RelaunchType.valueOf(type);
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Type value should be PROPOSAL or CONFIRMED");
      }
      return jpaRepository.getByInvoiceIdAndType(invoiceId, enumType, pageable).stream()
          .map(
              invoiceRelaunch ->
                  mapper.toDomain(invoiceRelaunch, invoiceRepository.getById(invoiceId)))
          .toList();
    }
  }

  @Override
  public InvoiceRelaunch save(
      Invoice invoice, String object, String htmlBody, boolean isUserRelaunched) {
    HInvoice invoiceEntity =
        invoiceJpaRepository
            .findById(invoice.getId())
            .orElseThrow(
                () -> new NotFoundException("Invoice(id=" + invoice.getId() + ") not found"));
    HInvoiceRelaunch toSave = mapper.toEntity(invoiceEntity, object, htmlBody, isUserRelaunched);
    HInvoiceRelaunch savedRelaunch = jpaRepository.save(toSave);
    return mapper.toDomain(savedRelaunch, invoiceRepository.getById(invoice.getId()));
  }
}
