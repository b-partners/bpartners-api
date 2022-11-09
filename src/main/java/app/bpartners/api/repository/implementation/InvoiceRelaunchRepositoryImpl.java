package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.RelaunchType;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.InvoiceRelaunchMapper;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import app.bpartners.api.repository.jpa.InvoiceRelaunchJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class InvoiceRelaunchRepositoryImpl implements InvoiceRelaunchRepository {
  private final InvoiceRelaunchJpaRepository invoiceRelaunchJpaRepository;
  private final InvoiceRelaunchMapper invoiceRelaunchMapper;

  @Override
  public List<InvoiceRelaunch> getByInvoiceId(
      String invoiceId, String type, Pageable pageable) {
    if (type == null) {
      return invoiceRelaunchJpaRepository
          .getByInvoiceId(invoiceId, pageable)
          .stream().map(invoiceRelaunchMapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    } else {
      RelaunchType enumType;
      try {
        enumType = RelaunchType.valueOf(type);
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Type value should be PROPOSAL or CONFIRMED");
      }
      return invoiceRelaunchJpaRepository
          .getByInvoiceIdAndType(invoiceId, enumType, pageable)
          .stream().map(invoiceRelaunchMapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    }
  }

  @Override
  public InvoiceRelaunch save(Invoice invoice) {
    HInvoiceRelaunch toSave = invoiceRelaunchMapper.toEntity(invoice);
    return invoiceRelaunchMapper
        .toDomain(invoiceRelaunchJpaRepository.save(toSave));
  }
}
