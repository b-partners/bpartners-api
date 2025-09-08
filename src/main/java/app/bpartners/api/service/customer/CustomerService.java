package app.bpartners.api.service.customer;

import static app.bpartners.api.endpoint.event.EventProducer.Conf.MAX_PUT_EVENT_ENTRIES;
import static app.bpartners.api.service.utils.CustomerUtils.getCustomersInfoFromFile;
import static java.util.stream.Collectors.toList;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.model.CustomerCrupdated;
import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.ban.BanApi;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class CustomerService {
  public static final String TEXT_CSV_MIME_TYPE = "text/csv";
  public static final String EXCEL_MIME_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  public static final String CSV_HEADERS =
      "ID,Nom,Prénom,Email,Site web,Adresse,Code postal,Ville,Pays,Commentaire,Type";
  private final CustomerRepository repository;
  private final CustomerRestMapper restMapper;
  private final EventProducer eventProducer;
  private final SesConf sesConf;
  private final BanApi banApi;

  private static String replaceNullValue(String value) {
    return value == null ? "" : value;
  }

  public void exportCustomers(String idUser, String fileType, PrintWriter pw) {
    var customers = repository.findAllByIdUserOrderByLastNameAsc(idUser);
    pw.println(CSV_HEADERS);
    customers.forEach(
        customer -> {
          pw.println(
              replaceNullValue(customer.getId())
                  + ","
                  + replaceNullValue(customer.getLastName())
                  + ","
                  + replaceNullValue(customer.getFirstName())
                  + ","
                  + replaceNullValue(customer.getEmail())
                  + ","
                  + replaceNullValue(customer.getWebsite())
                  + ","
                  + replaceNullValue(customer.getAddress())
                  + ","
                  + replaceNullValue(String.valueOf(customer.getZipCode()))
                  + ","
                  + replaceNullValue(customer.getCity())
                  + ","
                  + replaceNullValue(customer.getCountry())
                  + ","
                  + replaceNullValue(customer.getComment())
                  + ","
                  + replaceNullValue(customer.getTranslatedType()));
        });
  }

  public List<Customer> getCustomers(
      String idUser,
      String firstName,
      String lastName,
      String email,
      String phoneNumber,
      String city,
      String country,
      List<String> filters,
      String prospectId,
      CustomerStatus status,
      PageFromOne page,
      BoundedPageSize pageSize) {
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    List<String> keywords = new ArrayList<>();
    if (filters != null && !filters.isEmpty()) {
      keywords.addAll(filters.stream().map(String::toLowerCase).collect(toList()));
    }
    return repository.findByIdUserAndCriteria(
        idUser,
        firstName,
        lastName,
        email,
        phoneNumber,
        city,
        country,
        keywords,
        prospectId,
        status,
        pageValue,
        pageSizeValue);
  }

  public Customer getCustomerById(String id) {
    return repository.findById(id);
  }

  public Optional<Customer> getByProspectId(String prospectId) {
    return repository.findOptionalByProspectId(prospectId);
  }

  @Transactional
  public List<Customer> crupdateCustomers(User owner, List<Customer> customers) {
    List<Customer> saved = repository.saveAll(customers);

    List<Object> typedEvent =
        saved.isEmpty()
            ? List.of()
            : saved.stream()
                .map(customer -> toTypedEvent(owner, customer, customer.isRecentlyAdded()))
                .collect(toList());
    int typedEventSize = typedEvent.size();
    if (typedEventSize > MAX_PUT_EVENT_ENTRIES) {
      int subdivision = (int) Math.ceil(typedEventSize / (double) MAX_PUT_EVENT_ENTRIES);
      for (int i = 1; i <= subdivision; i++) {
        int firstIndex = i == 1 ? 0 : ((i - 1) * MAX_PUT_EVENT_ENTRIES);
        int afterLastIndex = i == subdivision ? typedEventSize : (i * MAX_PUT_EVENT_ENTRIES);
        eventProducer.accept(typedEvent.subList(firstIndex, afterLastIndex));
      }
    } else {
      eventProducer.accept(typedEvent); // TODO: add appropriate test
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
        .collect(toList());
  }

  private CustomerCrupdated toTypedEvent(User user, Customer customer, boolean isNew) {
    String subject =
        isNew
            ? "Ajout du nouveau client "
                + customer.getFullName()
                + " par l'artisan "
                + user.getName()
            : "Modification du client existant "
                + customer.getFullName()
                + " par l'artisan "
                + user.getName();
    String recipientEmail = sesConf.getAdminEmail();
    return new CustomerCrupdated()
        .subject(subject)
        .recipientEmail(recipientEmail)
        .type(isNew ? CustomerCrupdated.Type.CREATE : CustomerCrupdated.Type.UPDATE)
        .user(user)
        .customer(customer);
  }

  @Transactional
  public List<Customer> findByAccountHolderId(String accountHolderId) {
    return repository.findByIdAccountHolder(accountHolderId);
  }
}
