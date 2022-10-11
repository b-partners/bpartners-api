package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.mapper.AccountHolderMapper;
import app.bpartners.api.repository.AccountHolderRepository;
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

  @Override
  public List<AccountHolder> getAccountHolders() {
    return accountHolderSwanRepository.getAccountHolders().stream().map(
        accountHolderMapper::toDomain).collect(Collectors.toUnmodifiableList());
  }
}
