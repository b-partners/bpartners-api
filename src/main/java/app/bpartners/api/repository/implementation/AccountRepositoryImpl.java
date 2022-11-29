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
  private AccountSwanRepository swanRepository;
  private AccountMapper mapper;

  @Override
  public List<Account> findByBearer(String bearer) {
    return swanRepository.findByBearer(bearer).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Account findById(String accountId) {
    return swanRepository.findById(accountId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList())
        .get(0);
  }

  @Override
  public List<Account> findByUserId(String userId) {
    return swanRepository.findByUserId(userId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}
