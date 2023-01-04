package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.CustomerRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.service.utils.CustomerUtils.getCustomersInfoFromFile;
import static app.bpartners.api.service.utils.CustomerUtils.getExtension;

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

  @Transactional
  public List<Customer> crupdateCustomers(
      String accountId,
      List<Customer> customers) {
    return repository.saveAll(accountId, customers);
  }

  public List<CreateCustomer> getDataFromFile(File file) {
    try {
      Optional<String> fileExtension = getExtension(file.getName());
      if (fileExtension.isPresent() && !fileExtension.get().equals("xls")
          && !fileExtension.get().equals("xlsx")) {
        throw new BadRequestException("The uploaded file was neither .xls or .xlsx.");
      }
      return getCustomersInfoFromFile(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw new NotFoundException("File " + file.getName() + "not found.");
    }
  }

}
