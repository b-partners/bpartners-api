package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.UserInvoiceRelaunchConf;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceRelaunchConfMapper;
import app.bpartners.api.repository.UserInvoiceRelaunchConfRepository;
import app.bpartners.api.repository.jpa.AccountInvoiceRelaunchConfJpaRepository;
import app.bpartners.api.repository.jpa.model.HUserInvoiceRelaunchConf;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class UserInvoiceRelaunchConfRepositoryImpl
    implements UserInvoiceRelaunchConfRepository {
  private final AccountInvoiceRelaunchConfJpaRepository jpaRepository;
  private final InvoiceRelaunchConfMapper mapper;

  @Override
  public UserInvoiceRelaunchConf save(String idUser, UserInvoiceRelaunchConf domain) {
    HUserInvoiceRelaunchConf domainEntity = mapper.toEntity(idUser, domain);
    jpaRepository.findByIdUser(idUser)
        .ifPresent(relaunchConf -> domainEntity.setId(relaunchConf.getId()));
    return mapper.toDomain(jpaRepository.save(domainEntity));
  }

  @Override
  public UserInvoiceRelaunchConf getByIdUser(String idUser) {
    return mapper.toDomain(
        jpaRepository.findByIdUser(idUser).orElseThrow(
            () -> new NotFoundException(
                "There is no existing invoice relaunch config for User(id= " + idUser + ")")
        )
    );
  }
}
