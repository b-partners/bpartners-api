package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AccountHolderMapper;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HUser;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class AccountHolderRepositoryImpl implements AccountHolderRepository {
  private final AccountHolderMapper mapper;
  private final AccountHolderJpaRepository jpaRepository;
  private final UserJpaRepository userJpaRepository;

  /*TODO: user findAllByUserId instead*/
  @Override
  public List<AccountHolder> findAllByAccountId(String accountId) {
    HUser user = userJpaRepository.getByAccountId(accountId);
    if (user == null) {
      throw new NotFoundException("Account(id=" + accountId + ") is not associated to an user");
    }
    return jpaRepository.findAllByIdUser(user.getId()).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<AccountHolder> findAllByUserId(String userId) {
    return jpaRepository.findAllByIdUser(userId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public AccountHolder save(AccountHolder accountHolder) {
    HAccountHolder toSave = mapper.toEntity(accountHolder);
    HAccountHolder savedAccountHolder = jpaRepository.save(toSave);
    return mapper.toDomain(savedAccountHolder);
  }

  @Override
  public AccountHolder findById(String idAccountHolder) {
    return mapper.toDomain(
        jpaRepository.findById(idAccountHolder)
            .orElseThrow(
                () -> new NotFoundException("AccountHolder.id=" + idAccountHolder + " not found.")
            ));
  }
}
