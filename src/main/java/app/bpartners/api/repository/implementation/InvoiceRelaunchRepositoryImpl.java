package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.InvoiceRelaunch;
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
  public List<InvoiceRelaunch> getInvoiceRelaunchesByInvoiceIdAndCriteria(
      String invoiceId,
      Boolean isUserRelaunched,
      Pageable pageable
  ) {
    List<HInvoiceRelaunch> persistedList;
    if (isUserRelaunched == null) {
      persistedList = invoiceRelaunchJpaRepository
          .findAllByInvoiceId(invoiceId, pageable);
    } else {
      persistedList = invoiceRelaunchJpaRepository
          .findAllByInvoiceIdAndUserRelaunched(invoiceId, pageable, isUserRelaunched);
    }
    return persistedList
        .stream().map(invoiceRelaunchMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public InvoiceRelaunch save(String invoiceId) {
    HInvoiceRelaunch toSave = invoiceRelaunchMapper.toEntity(invoiceId);
    return invoiceRelaunchMapper
        .toDomain(invoiceRelaunchJpaRepository.save(toSave));
  }
}
