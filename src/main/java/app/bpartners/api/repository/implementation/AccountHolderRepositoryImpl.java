package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.CompanyInfo;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AccountHolderMapper;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import java.util.List;
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
  public List<AccountHolder> getByAccountId(String accountId) {
    HAccountHolder persisted = jpaRepository.findByAccountId(accountId).get(0);
    return swanRepository.getAccountHolders().stream()
        .map(swanAccountHolder ->
            mapper.toDomain(swanAccountHolder, persisted))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public AccountHolder save(String accountHolderId, CompanyInfo companyInfo) {
    HAccountHolder entity = jpaRepository
        .findById(accountHolderId)
        .orElseThrow(() -> new NotFoundException(
            "AccountHolder." + accountHolderId + " not found"
        ));
    if (companyInfo.getPhone() != null) {
      entity.setMobilePhoneNumber(companyInfo.getPhone());
    }
    if (companyInfo.getEmail() != null) {
      entity.setEmail(companyInfo.getEmail());
    }
    if (companyInfo.getTvaNumber() != null) {
      entity.setTvaNumber(companyInfo.getTvaNumber());
    }
    if (companyInfo.getSocialCapital() != null) {
      entity.setSocialCapital(companyInfo.getSocialCapital());
    }
    app.bpartners.api.repository.swan.model.AccountHolder swanAccountHolder =
        swanRepository.getAccountHolders().get(0);
    return mapper.toDomain(
        swanAccountHolder, jpaRepository.save(entity));
  }

  @Override
  public AccountHolder getById(String id) {
    HAccountHolder entity = jpaRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException(
            "AccountHolder." + id + " not found"
        ));
    app.bpartners.api.repository.swan.model.AccountHolder swanAccountHolder =
        swanRepository.getAccountHolders().get(0);
    return mapper.toDomain(
        swanAccountHolder, entity);
  }
}
