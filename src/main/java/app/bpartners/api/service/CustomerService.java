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
import app.bpartners.api.model.Location;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
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
  public static final String TEXT_CSV_MIME_TYPE = "text/csv";
  public static final String EXCEL_MIME_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  public static final String CSV_HEADERS =
      "ID,Nom,Prénom,Email,Site web,Adresse,Code postal,Ville,Pays,Commentaire";
  private final CustomerRepository repository;
  private final CustomerRestMapper restMapper;
  private final EventProducer eventProducer;
  private final EventConf eventConf;
  private final BanApi banApi;

  public void exportCustomers(String idUser, String fileType, PrintWriter pw) {
    var customers = repository.findAllByIdUserOrderByLastNameAsc(idUser);
    pw.println(CSV_HEADERS);
    customers.forEach(customer -> {
      pw.println(
          replaceNullValue(customer.getId()) + "," + replaceNullValue(customer.getLastName())
              + "," + replaceNullValue(customer.getFirstName()) + ","
              + replaceNullValue(customer.getEmail())
              + "," + replaceNullValue(customer.getWebsite()) + ","
              + replaceNullValue(customer.getAddress())
              + "," + replaceNullValue(String.valueOf(customer.getZipCode())) + ","
              + replaceNullValue(customer.getCity())
              + "," + replaceNullValue(customer.getCountry()) + ","
              + replaceNullValue(customer.getComment()));
    });
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

  @Scheduled(cron = Scheduled.CRON_DISABLED)
  public void updateCustomersLocation() {
    List<Customer> customers = repository.findWhereLatitudeOrLongitudeIsNull();
    int customersCount = customers.size();
    if (customersCount > 0) {
      log.warn("{} customers are to be updated on their latitude and longitude", customersCount);
    }
    StringBuilder sb = new StringBuilder();
    customers.forEach(customer -> {
      if (customer.getAddress() != null) {
        try {
          String fullAddress = customer.getFullAddress();
          if (fullAddress.length() < 3 || fullAddress.length() > 200) {
            sb.append(
                String.format(
                    "Unable to update Customer(id=%s,name=%s) position because address was %s",
                    customer.getId(), customer.getName(), fullAddress));
          } else {
            GeoPosition position = banApi.search(fullAddress);
            if (position == null) {
              sb.append("Customer(id=")
                  .append(customer.getId())
                  .append(") location was not updated because")
                  .append(" address ")
                  .append(fullAddress)
                  .append(" was not found");
            } else {
              Location newLocation = Location.builder()
                  .latitude(position.getCoordinates().getLatitude())
                  .longitude(position.getCoordinates().getLongitude())
                  .build();
              customer.setLocation(newLocation);
              repository.save(customer);
            }
          }
        } catch (BadRequestException | NotFoundException e) {
          sb.append("Customer(id=")
              .append(customer.getId())
              .append(") location was not updated because ")
              .append(e.getMessage());
        }
      }
    });
    String exceptionMessage = sb.toString();
    if (!exceptionMessage.isEmpty()) {
      log.warn(exceptionMessage);
    }
  }

  @Transactional
  public List<Customer> findByAccountHolderId(String accountHolderId) {
    return repository.findByIdAccountHolder(accountHolderId);
  }

  private static String replaceNullValue(String value) {
    return value == null ? "" : value;
  }
}
