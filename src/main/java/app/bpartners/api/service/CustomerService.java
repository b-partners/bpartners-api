package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedCustomerCrupdated;
import app.bpartners.api.endpoint.event.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.gen.CustomerCrupdated;
import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.CustomerRepository;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.event.EventProducer.Conf.MAX_PUT_EVENT_ENTRIES;
import static app.bpartners.api.service.utils.CustomerUtils.getCustomersInfoFromFile;

@Service
@AllArgsConstructor
@Slf4j
public class CustomerService {
  private final CustomerRepository repository;
  private final CustomerRestMapper restMapper;
  private final EventProducer eventProducer;
  private final EventConf eventConf;

  private List<String> retrieveKeywords(String firstName,
                                        String lastName, String email, String phoneNumber,
                                        String city, String country, List<String> filters) {
    List<String> keywords = new ArrayList<>();
    if (firstName != null) {
      log.warn("DEPRECATED: query parameter firstName is still used for filtering customers. Use " +
          "the query parameter filters instead.");
      keywords.add(firstName.toLowerCase());
    }
    if (lastName != null) {
      log.warn("DEPRECATED: query parameter lastName is still used for filtering customers. Use " +
          "the query parameter filters instead.");
      keywords.add(lastName.toLowerCase());
    }
    if (email != null) {
      log.warn("DEPRECATED: query parameter email is still used for filtering customers. Use " +
          "the query parameter filters instead.");
      keywords.add(email.toLowerCase());
    }
    if (phoneNumber != null) {
      log.warn("DEPRECATED: query parameter phoneNumber is still used for filtering customers. " +
          "Use the query parameter filters instead.");
      keywords.add(phoneNumber.toLowerCase());
    }
    if (city != null) {
      log.warn("DEPRECATED: query parameter city is still used for filtering customers. Use " +
          "the query parameter filters instead.");
      keywords.add(city.toLowerCase());
    }
    if (country != null) {
      log.warn("DEPRECATED: query parameter country is still used for filtering customers. Use " +
          "the query parameter filters instead.");
      keywords.add(country.toLowerCase());
    }
    if (filters != null && !filters.isEmpty()) {
      keywords.addAll(filters.stream()
          .map(String::toLowerCase)
          .collect(Collectors.toList()));
    }
    return keywords;
  }

  public List<Customer> getCustomers(String idUser, String firstName, String lastName, String email,
                                     String phoneNumber, String city, String country,
                                     List<String> filters, CustomerStatus status, PageFromOne page,
                                     BoundedPageSize pageSize) {
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    List<String> keywords = new ArrayList<>();
    if (filters != null && !filters.isEmpty()) {
      keywords.addAll(filters.stream()
          .map(String::toLowerCase)
          .collect(Collectors.toList()));
    }
    return repository.findByIdUserAndCriteria(idUser, firstName, lastName, email,
        phoneNumber, city, country, keywords, status, pageValue, pageSizeValue);
  }

  public Customer getCustomerById(String id) {
    return repository.findById(id);
  }

  @Transactional
  public List<Customer> crupdateCustomers(List<Customer> customers) {
    List<Customer> saved = repository.saveAll(customers);

    List<TypedEvent> typedEvent = saved.isEmpty() ? List.of()
        : saved.stream().map(customer -> {
          User user = AuthProvider.getAuthenticatedUser();
          return toTypedEvent(user, customer, customer.isRecentlyAdded());
        })
        .collect(Collectors.toUnmodifiableList());
    int typedEventSize = typedEvent.size();
    if (typedEventSize > MAX_PUT_EVENT_ENTRIES) {
      int subdivision = (int) Math.ceil(typedEventSize / (double) MAX_PUT_EVENT_ENTRIES);
      for (int i = 1; i <= subdivision; i++) {
        int firstIndex = i == 1 ? 0 : ((i - 1) * MAX_PUT_EVENT_ENTRIES);
        int afterLastIndex = i == subdivision ? typedEventSize : (i * MAX_PUT_EVENT_ENTRIES);
        eventProducer.accept(typedEvent.subList(firstIndex, afterLastIndex));
      }
    } else {
      eventProducer.accept(typedEvent); //TODO: add appropriate test
    }

    return saved;
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

  public void checkCustomerExistence(Customer toCheck) {
    if (repository.findOptionalById(toCheck.getId()).isEmpty()) {
      throw new NotFoundException(
          "Customer(id=" + toCheck.getId() + ") not found");
    }
  }

  private TypedCustomerCrupdated toTypedEvent(User user, Customer customer, boolean isNew) {
    String subject = isNew
        ? "Ajout du nouveau client " + customer.getName() + " par l'artisan " + user.getName()
        : "Modification du client existant " + customer.getName() + " par l'artisan "
        + user.getName();
    String recipientEmail = eventConf.getAdminEmail();
    return new TypedCustomerCrupdated(
        new CustomerCrupdated()
            .subject(subject)
            .recipientEmail(recipientEmail)
            .type(isNew ? CustomerCrupdated.Type.CREATE
                : CustomerCrupdated.Type.UPDATE)
            .user(user)
            .customer(customer)
    );
  }
}
