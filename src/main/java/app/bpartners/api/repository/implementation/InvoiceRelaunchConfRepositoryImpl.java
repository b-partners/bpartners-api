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

@Repository
@AllArgsConstructor
public class InvoiceRelaunchConfRepositoryImpl implements InvoiceRelaunchConfRepository {
  private final InvoiceRelaunchConfJpaRepository jpaRepository;
  private final InvoiceRelaunchConfMapper mapper;

  @Override
  public InvoiceRelaunchConf findByInvoiceId(String idInvoice) {
    return mapper.toDomain(
        jpaRepository
            .findByIdInvoice(idInvoice)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "No relaunch configuration found for" + " Invoice." + idInvoice)));
  }

  @Override
  public InvoiceRelaunchConf save(InvoiceRelaunchConf invoiceRelaunchConf) {
    Optional<HInvoiceRelaunchConf> optionalPersisted =
        jpaRepository.findByIdInvoice(invoiceRelaunchConf.getIdInvoice());
    if (optionalPersisted.isPresent()) {
      HInvoiceRelaunchConf persisted = optionalPersisted.get();
      persisted.setDelay(invoiceRelaunchConf.getDelay());
      persisted.setRehearsalNumber(invoiceRelaunchConf.getRehearsalNumber());
      return mapper.toDomain(jpaRepository.save(persisted));
    }
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(invoiceRelaunchConf)));
  }
}
