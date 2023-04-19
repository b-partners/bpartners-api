package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.exception.NotFoundException;
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
    if (swanAccountHolders.isEmpty()) {
      return jpaRepository.findAllByAccountId(accountId).stream()
          .map(mapper::toDomain)
          .collect(Collectors.toList());
    }
    return swanAccountHolders.stream()
        .map(swanAccountHolder -> getUpdatedAccountHolder(accountId, swanAccountHolder))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public AccountHolder save(AccountHolder accountHolder) {
    HAccountHolder toSave = mapper.toEntity(accountHolder);
    HAccountHolder savedAccountHolder = jpaRepository.save(toSave);
    return mapper.toDomain(savedAccountHolder);
  }

  //TODO: check why by ID and account ID
  @Override
  public AccountHolder getByIdAndAccountId(String id, String accountId) {
    SwanAccountHolder swanAccountHolder = swanRepository.getById(id);
    Optional<HAccountHolder> optionalAccountHolder =
        jpaRepository.findByIdAndAccountId(id, accountId);
    if (optionalAccountHolder.isEmpty()) {
      if (swanAccountHolder != null) {
        return mapper.toDomain(
            jpaRepository.save(getUpdatedEntity(accountId, swanAccountHolder)));
      }
    } else {
      return mapper.toDomain(optionalAccountHolder.get());
    }
    throw new NotFoundException("AccountHolder.id=" + id + " not found.");
  }

  @Override
  public AccountHolder findById(String idAccountHolder) {
    return mapper.toDomain(
        jpaRepository.findById(idAccountHolder)
            .orElseThrow(
                () -> new NotFoundException("AccountHolder.id=" + idAccountHolder + " not found.")
            ));
  }

  //TODO: when multiple accounts are supported, we should handle n<acHold> ... n<ac>
  public HAccountHolder getUpdatedEntity(String accountId, SwanAccountHolder swanAccountHolder) {
    Optional<HAccountHolder> optionalAccountHolder = jpaRepository.findByAccountId(accountId);
    if (optionalAccountHolder.isEmpty()) {
      return jpaRepository.save(mapper.toEntity(accountId, swanAccountHolder));
    }
    HAccountHolder entity = optionalAccountHolder.get();
    checkAccountHolderUpdates(swanAccountHolder, entity);
    return jpaRepository.save(entity);
  }

  private AccountHolder getUpdatedAccountHolder(
      String accountId, SwanAccountHolder swanAccountHolder) {
    return mapper.toDomain(getUpdatedEntity(accountId, swanAccountHolder));
  }

  private void checkAccountHolderUpdates(
      SwanAccountHolder swanAccountHolder, HAccountHolder entity) {
    if (entity.getVerificationStatus() == null || (!entity.getVerificationStatus()
        .getValue().equals(swanAccountHolder.getVerificationStatus()))) {
      entity.setVerificationStatus(
          mapper.getStatus(swanAccountHolder.getVerificationStatus()));
    }
    if (entity.getName() == null || (!entity.getName()
        .equals(swanAccountHolder.getInfo().getName()))) {
      entity.setName(swanAccountHolder.getInfo().getName());
    }
    if (entity.getRegistrationNumber() == null || (!entity.getRegistrationNumber()
        .equals(swanAccountHolder.getInfo().getRegistrationNumber()))) {
      entity.setRegistrationNumber(swanAccountHolder.getInfo().getRegistrationNumber());
    }
    if (entity.getBusinessActivity() == null || (!entity.getBusinessActivity()
        .equals(swanAccountHolder.getInfo().getBusinessActivity()))) {
      entity.setBusinessActivity(swanAccountHolder.getInfo().getBusinessActivity());
    }
    if (entity.getBusinessActivityDescription() == null || (!entity.getName()
        .equals(swanAccountHolder.getInfo().getBusinessActivityDescription()))) {
      entity.setBusinessActivityDescription(
          swanAccountHolder.getInfo().getBusinessActivityDescription());
    }
    if (entity.getAddress() == null || (!entity.getAddress()
        .equals(swanAccountHolder.getResidencyAddress().getAddressLine1()))) {
      entity.setAddress(swanAccountHolder.getResidencyAddress().getAddressLine1());
    }
    if (entity.getCity() == null || (!entity.getCity()
        .equals(swanAccountHolder.getResidencyAddress().getCity()))) {
      entity.setCity(swanAccountHolder.getResidencyAddress().getCity());
    }
    if (entity.getCountry() == null || (!entity.getCountry()
        .equals(swanAccountHolder.getResidencyAddress().getCountry()))) {
      entity.setCountry(swanAccountHolder.getResidencyAddress().getCountry());
    }
    if (entity.getPostalCode() == null || (!entity.getPostalCode()
        .equals(swanAccountHolder.getResidencyAddress().getPostalCode()))) {
      entity.setPostalCode(swanAccountHolder.getResidencyAddress().getPostalCode());
    }
  }
}
