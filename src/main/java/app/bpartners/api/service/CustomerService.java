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
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
  private final BanApi banApi;

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

  @Scheduled(cron = "0 10 * * * *")
  public void updateCustomersLocation() {
    List<Customer> customersToUpdate = repository.findWhereLatitudeOrLongitudeIsNull();
    log.warn("{} customers are to be updated on their latitude and longitude",
        customersToUpdate.size());
    customersToUpdate.forEach(
        customer -> {
          GeoPosition position = banApi.search(customer.getAddress());
          customer.setLatitude(position.getCoordinates().getLatitude());
          customer.setLongitude(position.getCoordinates().getLongitude());
        });
  }
}
