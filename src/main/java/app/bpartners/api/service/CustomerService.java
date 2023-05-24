package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.repository.CustomerRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.bpartners.api.service.utils.CustomerUtils.getCustomersInfoFromFile;

@Service
@AllArgsConstructor
@Slf4j
public class CustomerService {
  private final CustomerRepository repository;
  private final CustomerRestMapper restMapper;

  public List<Customer> getCustomers(
      String idUser, String firstName,
      String lastName, String email, String phoneNumber, String city, String country,
      CustomerStatus status,
      PageFromOne page, BoundedPageSize pageSize) {
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    return repository.findByIdUserAndCriteria(idUser, firstName, lastName, email,
        phoneNumber, city, country, status, pageValue, pageSizeValue);
  }

  public Customer getCustomerById(String id) {
    return repository.findById(id);
  }

  @Transactional
  public List<Customer> crupdateCustomers(List<Customer> customers) {
    return repository.saveAll(customers);
  }

  public List<Customer> updateStatuses(List<UpdateCustomerStatus> customerStatusList) {
    return repository.updateCustomersStatuses(customerStatusList);
  }

  public List<Customer> getDataFromFile(String idUser, byte[] file) {
    List<CreateCustomer> customersFromFile =
        getCustomersInfoFromFile(new ByteArrayInputStream(file));
    return customersFromFile.stream()
        .map(customer -> restMapper.toDomain(idUser, customer))
        .collect(Collectors.toList());
  }
}
