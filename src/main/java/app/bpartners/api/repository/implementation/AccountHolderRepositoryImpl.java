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
    List<SwanAccountHolder> swanAccountHolders = swanRepository.findAllByAccountId(accountId);
    List<AccountHolder> accountHolders = jpaRepository.findAllByAccountId(accountId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    List<String> accountHoldersId = accountHolders.stream()
        .map(AccountHolder::getId)
        .collect(Collectors.toUnmodifiableList());
    if (!accountHolders.isEmpty()) {
      List<AccountHolder> saved = jpaRepository.saveAll(swanAccountHolders.stream()
              .filter(swanAccountHolder -> !accountHoldersId.contains(swanAccountHolder.getId()))
              .map(swanAccountHolder -> getOrCreateAccountHolderEntity(accountId, swanAccountHolder))
              .collect(Collectors.toUnmodifiableList()))
          .stream()
          .map(mapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
      accountHolders.addAll(saved);
      return accountHolders;
    }
    return jpaRepository.saveAll(swanAccountHolders.stream()
            .map(swanAccountHolder -> getOrPersistAccountHolder(accountId, swanAccountHolder))
            .map(mapper::toEntity)
            .collect(Collectors.toUnmodifiableList()))
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());

  }

  @Override
  public AccountHolder save(AccountHolder accountHolder) {
    HAccountHolder entity = mapper.toEntity(accountHolder);
    return mapper.toDomain(jpaRepository.save(entity));
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
          .name(swanAccountHolder.getInfo().getName())
          .businessActivity(swanAccountHolder.getInfo().getBusinessActivity())
          .businessActivityDescription(
              swanAccountHolder.getInfo().getBusinessActivityDescription())
          .registrationNumber(swanAccountHolder.getInfo().getRegistrationNumber())
          .address(swanAccountHolder.getResidencyAddress().getAddressLine1())
          .city(swanAccountHolder.getResidencyAddress().getCity())
          .country(swanAccountHolder.getResidencyAddress().getCountry())
          .postalCode(swanAccountHolder.getResidencyAddress().getPostalCode())
          .initialCashflow(String.valueOf(0))
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
    Optional<HAccountHolder> accountHolder = jpaRepository.findByIdAndAccountId(id, accountId);
    if (!accountHolder.isPresent()) {
      return mapper.toDomain(
          jpaRepository.save(getOrCreateAccountHolderEntity(accountId, swanAccountHolder)));
    }
    return mapper.toDomain(accountHolder.get());
  }

  public AccountHolder getOrPersistAccountHolder(
      String accountId, SwanAccountHolder swanAccountHolder) {
    HAccountHolder entity = getOrCreateAccountHolderEntity(accountId, swanAccountHolder);
    return mapper.toDomain(entity);
  }
}
