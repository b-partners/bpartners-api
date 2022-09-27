package app.bpartners.api.service;

import app.bpartners.api.model.CustomerTemplate;
import app.bpartners.api.repository.CustomerRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {
  private final CustomerRepository repository;

  public List<CustomerTemplate> getCustomers(String accountId, String name) {
    if (name == null) {
      return repository.findByAccount(accountId);
    }
    return repository.findByAccountIdAndName(accountId, name);
  }

  public List<CustomerTemplate> createCustomers(
      String accountId,
      List<CustomerTemplate> customerTemplates) {
    return repository.save(accountId, customerTemplates);
  }
}
