package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.CustomerCrupdated;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Location;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import static app.bpartners.api.service.utils.TemplateResolverUtils.parseTemplateResolver;

@Service
@AllArgsConstructor
@Slf4j
public class CustomerCrupdatedService implements Consumer<CustomerCrupdated> {
  public static final String CUSTOMER_CRUPDATED_MAIL = "customer_crupdated_mail";
  private final SesService service;
  private final CustomerRepository customerRepository;
  private final BanApi banApi;

  public void accept(List<CustomerCrupdated> customerCrupdatedList) {
    customerCrupdatedList.forEach(this);
  }

  @Override
  public void accept(CustomerCrupdated customerCrupdated) {
    String subject = customerCrupdated.getSubject();
    String recipient = customerCrupdated.getRecipientEmail();
    Customer customer = customerCrupdated.getCustomer();
    CustomerCrupdated.Type type = customerCrupdated.getType();
    List<Attachment> attachments = List.of();

    Customer updatedCustomer = customer.toBuilder().build();
    if (!updatedCustomer.getFullAddress().equals(updatedCustomer.getLatestFullAddress())) {
      GeoPosition customerPosition = banApi.fSearch(customer.getAddress());
      if (customerPosition != null
          && !customerPosition.getCoordinates().equals(
          customer.getLocation().getCoordinate())) {
        updatedCustomer = customerRepository.save(customer.toBuilder()
            .location(Location.builder()
                .coordinate(customerPosition.getCoordinates())
                .address(customer.getAddress())
                .longitude(customerPosition.getCoordinates().getLongitude())
                .latitude(customerPosition.getCoordinates().getLatitude())
                .build())
            .build());
        log.info("{} coordinates updated from BAN API", updatedCustomer.describe());
      }
    }

    String
        htmlBody = parseTemplateResolver(CUSTOMER_CRUPDATED_MAIL,
        configureCustomerContext(customerCrupdated.getUser(), updatedCustomer,
            type));
    try {
      service.sendEmail(recipient, null, subject, htmlBody, attachments);
      log.info("Email sent to {} to notify {} update", recipient, updatedCustomer.describe());
    } catch (MessagingException | IOException e) {
      log.error("Email not sent : " + e.getMessage());
    }
  }

  private Context configureCustomerContext(User user, Customer customer,
                                           CustomerCrupdated.Type type) {
    Context context = new Context();
    context.setVariable("type", type.toString());
    context.setVariable("user", user);
    context.setVariable("customer", customer);
    return context;
  }
}
