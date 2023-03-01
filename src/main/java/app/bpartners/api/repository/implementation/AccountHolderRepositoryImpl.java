package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.mapper.AccountHolderMapper;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AccountHolderRepositoryImpl implements AccountHolderRepository {
  private final AccountHolderSwanRepository swanRepository;
  private final AccountHolderMapper mapper;
  private final AccountHolderJpaRepository jpaRepository;

  @Override
  public List<AccountHolder> findAllByAccountId(String accountId) {
    return swanRepository.findAllByAccountId(accountId).stream()
        .map(swanAccountHolder -> getOrPersistAccountHolder(accountId, swanAccountHolder))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public AccountHolder save(AccountHolder accountHolder) {
    SwanAccountHolder swanAccountHolder =
        swanRepository.getById(accountHolder.getId());
    HAccountHolder entity = mapper.toEntity(accountHolder);
    return mapper.toDomain(swanAccountHolder, jpaRepository.save(entity));
  }

  public HAccountHolder getOrCreateAccountHolderEntity(
      String accountId,
      SwanAccountHolder swanAccountHolder) {
    Optional<HAccountHolder> optional = jpaRepository.findByAccountId(accountId);
    HAccountHolder entity;
    if (optional.isEmpty()) {
      entity = jpaRepository.save(HAccountHolder.builder()
          .id(swanAccountHolder.getId())
          .accountId(accountId)
          .subjectToVat(true) //By default, an account holder IS subject to vat
          .mobilePhoneNumber(null)
          .email(null)
          .socialCapital(0) //TODO : check default social capital 0 or null
          .vatNumber(swanAccountHolder.getInfo().getVatNumber())
          .build());
    } else {
      entity = optional.get();
    }
    return entity;
  }

  @Override
  public AccountHolder getByIdAndAccountId(String id, String accountId) {
    SwanAccountHolder swanAccountHolder =
        swanRepository.getById(id);
    return getOrPersistAccountHolder(accountId, swanAccountHolder);
  }

  public AccountHolder getOrPersistAccountHolder(
      String accountId, SwanAccountHolder swanAccountHolder) {
    HAccountHolder entity = getOrCreateAccountHolderEntity(accountId, swanAccountHolder);
    return mapper.toDomain(swanAccountHolder, entity);
  }
}
