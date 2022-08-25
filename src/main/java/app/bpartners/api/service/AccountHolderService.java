package app.bpartners.api.service;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.implementation.AccountHolderRepositoryImpl;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountHolderService {

  private final AccountHolderRepositoryImpl accountHolderRepository;

  public List<AccountHolder> getAccountHolders() {
    return accountHolderRepository.getAccountHolders();
  }
}
