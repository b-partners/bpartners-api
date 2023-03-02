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
    if (!swanAccountHolders.isEmpty()) {
      return swanAccountHolders.stream()
          .map(swanAccountHolder -> getOrPersistAccountHolder(accountId, swanAccountHolder))
          .collect(Collectors.toUnmodifiableList());
    }
    return jpaRepository.findAllByAccountId(accountId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
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
      entity = jpaRepository.save(mapper.toEntity(accountId, swanAccountHolder));
    } else {
      HAccountHolder optionalValue = optional.get();
      checkAccountHolderUpdates(swanAccountHolder, optionalValue);
      entity = jpaRepository.save(optionalValue);
    }
    return entity;
  }

  @Override
  public AccountHolder getByIdAndAccountId(String id, String accountId) {
    SwanAccountHolder swanAccountHolder =
        swanRepository.getById(id);
    Optional<HAccountHolder> accountHolder = jpaRepository.findByIdAndAccountId(id, accountId);
    if (accountHolder.isEmpty()) {
      return mapper.toDomain(
          jpaRepository.save(getOrCreateAccountHolderEntity(accountId, swanAccountHolder)));
    }
    return mapper.toDomain(accountHolder.get());
  }

  private AccountHolder getOrPersistAccountHolder(
      String accountId, SwanAccountHolder swanAccountHolder) {
    return mapper.toDomain(getOrCreateAccountHolderEntity(accountId, swanAccountHolder));
  }

  private void checkAccountHolderUpdates(SwanAccountHolder swanAccountHolder,
                                         HAccountHolder optionalValue) {
    if (optionalValue.getVerificationStatus() == null
        || (!optionalValue.getVerificationStatus().getValue()
        .equals(swanAccountHolder.getVerificationStatus()))) {
      optionalValue.setVerificationStatus(
          mapper.getStatus(swanAccountHolder.getVerificationStatus()));
    }
    if (optionalValue.getName() == null
        || (!optionalValue.getName().equals(swanAccountHolder.getInfo().getName()))) {
      optionalValue.setName(swanAccountHolder.getInfo().getName());
    }
    if (optionalValue.getRegistrationNumber() == null
        || (!optionalValue.getRegistrationNumber()
        .equals(swanAccountHolder.getInfo().getRegistrationNumber()))) {
      optionalValue.setRegistrationNumber(swanAccountHolder.getInfo().getRegistrationNumber());
    }
    if (optionalValue.getBusinessActivity() == null
        || (!optionalValue.getBusinessActivity()
        .equals(swanAccountHolder.getInfo().getBusinessActivity()))) {
      optionalValue.setBusinessActivity(swanAccountHolder.getInfo().getBusinessActivity());
    }
    if (optionalValue.getBusinessActivityDescription() == null
        || (!optionalValue.getName()
        .equals(swanAccountHolder.getInfo().getBusinessActivityDescription()))) {
      optionalValue.setBusinessActivityDescription(
          swanAccountHolder.getInfo().getBusinessActivityDescription());
    }
    if (optionalValue.getAddress() == null
        || (!optionalValue.getAddress()
        .equals(swanAccountHolder.getResidencyAddress().getAddressLine1()))) {
      optionalValue.setAddress(swanAccountHolder.getResidencyAddress().getAddressLine1());
    }
    if (optionalValue.getCity() == null
        || (!optionalValue.getCity().equals(swanAccountHolder.getResidencyAddress().getCity()))) {
      optionalValue.setCity(swanAccountHolder.getResidencyAddress().getCity());
    }
    if (optionalValue.getCountry() == null
        || (!optionalValue.getCountry()
        .equals(swanAccountHolder.getResidencyAddress().getCountry()))) {
      optionalValue.setCountry(swanAccountHolder.getResidencyAddress().getCountry());
    }
    if (optionalValue.getPostalCode() == null
        || (!optionalValue.getPostalCode()
        .equals(swanAccountHolder.getResidencyAddress().getPostalCode()))) {
      optionalValue.setPostalCode(swanAccountHolder.getResidencyAddress().getPostalCode());
    }
  }


}
