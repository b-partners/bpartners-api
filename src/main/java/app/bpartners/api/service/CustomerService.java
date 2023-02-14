package app.bpartners.api.service;

import app.bpartners.api.model.Customer;
import app.bpartners.api.repository.CustomerRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {
  private final CustomerRepository repository;

  public List<Customer> getCustomers(
      String accountId, String name, String firstName, String lastName) {
    if (name == null && firstName == null && lastName == null) {
      return repository.findByAccount(accountId);
    }
    if (name == null) {
      return repository.findByAccountIdAndName(accountId, firstName, lastName);
    }
    String[] splitedName = Objects.requireNonNull(name).split(" ");
    String firstNameFromName = splitedName[0];
    String lastNameFromName =
        String.join(" ", Arrays.asList(splitedName).subList(1, splitedName.length));
    return repository.findByAccountIdAndName(accountId, firstNameFromName, lastNameFromName);
  }

  @Transactional
  public List<Customer> crupdateCustomers(
      String accountId,
      List<Customer> customers) {
    return repository.saveAll(accountId, customers);
  }

}
