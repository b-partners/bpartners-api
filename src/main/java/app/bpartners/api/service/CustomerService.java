package app.bpartners.api.service;

import app.bpartners.api.model.CustomerTemplate;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.service.aws.SesService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {
  private final CustomerRepository repository;
  private final SesService mailService;

  public List<CustomerTemplate> getCustomers(String accountId, String name) {
    if (name == null) {
      return repository.findByAccount(accountId);
    }
    return repository.findByAccountIdAndName(accountId, name);
  }

  public List<CustomerTemplate> createCustomers(
      String accountId,
      List<CustomerTemplate> customerTemplates) {
    customerTemplates.forEach(
        customerTemplate -> mailService.addCustomer(customerTemplate.getEmail()));
    //TODO : update email on customer info update
    return repository.save(accountId, customerTemplates);
  }
}
