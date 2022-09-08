package app.bpartners.api.service;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.AccountHolderRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountHolderService {

  private final AccountHolderRepository accountHolderRepository;

  public List<AccountHolder> getAccountHolders() {
    return accountHolderRepository.getAccountHolders();
  }
}
