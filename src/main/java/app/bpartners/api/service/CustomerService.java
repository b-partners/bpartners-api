package app.bpartners.api.service;

import app.bpartners.api.model.Customer;
import app.bpartners.api.repository.CustomerRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {
  private final CustomerRepository repository;

  public List<Customer> getCustomers(String accountId, String name) {
    if (name == null) {
      return repository.findByAccount(accountId);
    }
    return repository.findByAccountIdAndName(accountId, name);
  }

  public List<Customer> createCustomers(String accountId, List<Customer> customers) {
    return repository.save(accountId, customers);
  }
}
