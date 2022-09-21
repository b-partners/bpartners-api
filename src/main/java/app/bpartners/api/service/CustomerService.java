package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.repository.CustomerRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {
  private final CustomerRepository repository;
  private final AccountService accountService;     //TODO: remove when SelfMatcher is set

  public List<Customer> getCustomers(String accountId, String name) {
    //TODO: remove when SelfMatcher is set
    Account authenticatedAccount = accountService.getAccounts().get(0);
    if (!authenticatedAccount.getId().equals(accountId)) {
      throw new ForbiddenException();
    }
    if (name == null) {
      return repository.findByAccount(accountId);
    }
    return repository.findByAccountIdAndName(accountId, name);
  }

  public List<Customer> createCustomers(String accountId, List<Customer> customers) {
    //TODO: remove when SelfMatcher is set
    Account authenticatedAccount = accountService.getAccounts().get(0);
    if (!authenticatedAccount.getId().equals(accountId)) {
      throw new ForbiddenException();
    }
    return repository.save(accountId, customers);
  }
}
