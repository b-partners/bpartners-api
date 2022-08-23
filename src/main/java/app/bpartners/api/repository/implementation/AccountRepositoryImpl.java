package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.mapper.SwanAccountMapper;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {
  private final AccountSwanRepository swanRepository;
  private final SwanAccountMapper mapper;

  @Override
  public Account getAccount() {
    return mapper.toDomain(swanRepository.getAccount());
  }
}
