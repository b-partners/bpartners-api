package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.repository.CustomerRepository;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.service.utils.CustomerUtils.getCustomersInfoFromFile;

@Service
@AllArgsConstructor
public class CustomerService {
  private final CustomerRepository repository;

  public List<Customer> getCustomers(
      String accountId, String name, String firstName,
      String lastName, PageFromOne page, BoundedPageSize pageSize) {
    int pageValue = page.getValue() - 1;
    int pageSizeValue = pageSize.getValue();
    if (name == null && firstName == null && lastName == null) {
      return repository.findByAccount(accountId, pageValue, pageSizeValue);
    }
    if (name == null) {
      return repository.findByAccountIdAndName(
          accountId, firstName, lastName, pageValue, pageSizeValue);
    }
    String[] splitedName = Objects.requireNonNull(name).split(" ");
    String firstNameFromName = splitedName[0];
    String lastNameFromName =
        String.join(" ", Arrays.asList(splitedName).subList(1, splitedName.length));
    return repository.findByAccountIdAndName(
        accountId, firstNameFromName, lastNameFromName, pageValue, pageSizeValue);
  }

  @Transactional
  public List<Customer> crupdateCustomers(
      String accountId,
      List<Customer> customers) {
    return repository.saveAll(accountId, customers);
  }

  public List<Customer> getDataFromFile(String accountId, byte[] file) {
    List<CreateCustomer> customersFromFile =
        getCustomersInfoFromFile(new ByteArrayInputStream(file))
            .stream().distinct()
            .collect(Collectors.toUnmodifiableList());
    return checkIfPersisted(accountId, customersFromFile);
  }

  private List<Customer> checkIfPersisted(String accountId, List<CreateCustomer> createCustomers) {
    List<Customer> toUpdateList = new ArrayList<>();
    List<Customer> toCreateList;
    List<Customer> persisted = repository.findByAccount(accountId, 1, Integer.MAX_VALUE);
    for (Customer customer : persisted) {
      for (CreateCustomer toCreate : createCustomers) {
        if (customer.getFirstName().equals(toCreate.getFirstName())
            && customer.getLastName().equals(toCreate.getLastName())) {
          customer.setEmail(toCreate.getEmail());
          customer.setPhone(toCreate.getPhone());
          customer.setWebsite(toCreate.getWebsite());
          customer.setAddress(toCreate.getAddress());
          customer.setCity(toCreate.getCity());
          customer.setCountry(toCreate.getCountry());
          customer.setComment(toCreate.getComment());
          toUpdateList.add(customer);
        }
      }
    }
    if (toUpdateList.isEmpty()) {
      toCreateList = createCustomers.stream()
          .map(createCustomer -> toDomainWithoutCheck(accountId, createCustomer))
          .collect(Collectors.toUnmodifiableList());
    } else {
      List<String> toUpdateName = toUpdateList.stream()
          .map(customer -> customer.getFirstName() + " " + customer.getLastName())
          .collect(Collectors.toUnmodifiableList());
      toCreateList = createCustomers.stream()
          .filter(createCustomer -> !toUpdateName.contains(
              createCustomer.getFirstName() + " " + createCustomer.getLastName()))
          .map(createCustomer -> toDomainWithoutCheck(accountId, createCustomer))
          .collect(Collectors.toUnmodifiableList());
    }
    toUpdateList.addAll(toCreateList);
    return toUpdateList;
  }

  //TODO: put this to CustomerRestMapper
  private Customer toDomainWithoutCheck(String accountId, CreateCustomer toCreate) {
    return Customer.builder()
        .idAccount(accountId)
        .firstName(toCreate.getFirstName())
        .lastName(toCreate.getLastName())
        .email(toCreate.getEmail())
        .address(toCreate.getAddress())
        .phone(toCreate.getPhone())
        .website(toCreate.getWebsite())
        .city(toCreate.getCity())
        .country(toCreate.getCountry())
        .comment(toCreate.getComment())
        .build();
  }
}
