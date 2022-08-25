package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {
  private AccountSwanRepository accountRepository;
  private AccountMapper mapper;

  @Override
  public List<Account> getAccounts() {
    return accountRepository.getAccounts().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}
