package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceRelaunchMapper;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import app.bpartners.api.repository.jpa.InvoiceRelaunchJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class InvoiceRelaunchRepositoryImpl implements InvoiceRelaunchRepository {
  private final InvoiceRelaunchJpaRepository jpaRepository;
  private final InvoiceRelaunchMapper mapper;

  @Override
  public InvoiceRelaunch save(InvoiceRelaunch invoiceRelaunch, String accountId) {
    Optional<HInvoiceRelaunch> optionalHInvoiceRelaunch = jpaRepository.getByAccountId(accountId);
    if (optionalHInvoiceRelaunch.isPresent()) {
      HInvoiceRelaunch persisted = optionalHInvoiceRelaunch.get();
      invoiceRelaunch.setId(persisted.getId());
    }
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(invoiceRelaunch)));
  }

  @Override
  public InvoiceRelaunch getByAccountId(String accountId) {
    return mapper.toDomain(
        jpaRepository.getByAccountId(accountId).orElseThrow(
            () -> new NotFoundException("No InvoiceRelaunch on this account")
        )
    );
  }
}
