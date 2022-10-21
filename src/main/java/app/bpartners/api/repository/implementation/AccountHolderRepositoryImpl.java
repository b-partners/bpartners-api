package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AccountHolder;
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
  private final AccountHolderSwanRepository accountHolderSwanRepository;
  private final AccountHolderMapper accountHolderMapper;

  private final AccountHolderJpaRepository accountHolderJpaRepository;

  @Override
  public List<AccountHolder> getAccountHolders() {
    //TODO: require validation for accountId attributes
    HAccountHolder entity = accountHolderJpaRepository
        .getHAccountHolderByAccountId("beed1765-5c16-472a-b3f4-5c376ce5db58");
    return accountHolderSwanRepository.getAccountHolders().stream()
        .map(elt -> accountHolderMapper.toDomain(elt, entity))
        .collect(Collectors.toUnmodifiableList());
  }

}
