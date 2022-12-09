package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AccountInvoiceRelaunchConf;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceRelaunchConfMapper;
import app.bpartners.api.repository.AccountInvoiceRelaunchConfRepository;
import app.bpartners.api.repository.jpa.AccountInvoiceRelaunchConfJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountInvoiceRelaunchConf;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class AccountInvoiceRelaunchConfRepositoryImpl
    implements AccountInvoiceRelaunchConfRepository {
  private final AccountInvoiceRelaunchConfJpaRepository jpaRepository;
  private final InvoiceRelaunchConfMapper mapper;

  @Override
  public AccountInvoiceRelaunchConf save(
      AccountInvoiceRelaunchConf accountInvoiceRelaunchConf,
      String accountId) {
    Optional<HAccountInvoiceRelaunchConf> optionalHInvoiceRelaunchConf =
        jpaRepository.getByAccountId(accountId);
    if (optionalHInvoiceRelaunchConf.isPresent()) {
      HAccountInvoiceRelaunchConf persisted = optionalHInvoiceRelaunchConf.get();
      accountInvoiceRelaunchConf.setId(persisted.getId());
    }
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(accountInvoiceRelaunchConf)));
  }

  @Override
  public AccountInvoiceRelaunchConf getByAccountId(String accountId) {
    return mapper.toDomain(
        jpaRepository.getByAccountId(accountId).orElseThrow(
            () -> new NotFoundException(
                "There is no existing invoice relaunch config for account. " + accountId)
        )
    );
  }
}
