package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceRelaunchConfMapper;
import app.bpartners.api.repository.InvoiceRelaunchConfRepository;
import app.bpartners.api.repository.jpa.InvoiceRelaunchConfJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunchConf;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class InvoiceRelaunchConfRepositoryImpl implements InvoiceRelaunchConfRepository {
  private final InvoiceRelaunchConfJpaRepository jpaRepository;
  private final InvoiceRelaunchConfMapper mapper;

  @Override
  public InvoiceRelaunchConf save(InvoiceRelaunchConf invoiceRelaunchConf, String accountId) {
    Optional<HInvoiceRelaunchConf> optionalHInvoiceRelaunchConf =
        jpaRepository.getByAccountId(accountId);
    if (optionalHInvoiceRelaunchConf.isPresent()) {
      HInvoiceRelaunchConf persisted = optionalHInvoiceRelaunchConf.get();
      invoiceRelaunchConf.setId(persisted.getId());
    }
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(invoiceRelaunchConf)));
  }

  @Override
  public InvoiceRelaunchConf getByAccountId(String accountId) {
    return mapper.toDomain(
        jpaRepository.getByAccountId(accountId).orElseThrow(
            () -> new NotFoundException(
                "There is no existing invoice relaunch config for account. " + accountId)
        )
    );
  }
}
