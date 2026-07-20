package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.event.model.CustomerCrupdated.Type.CREATE;

import app.bpartners.api.endpoint.event.model.CustomerCrupdated;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Location;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
@Slf4j
public class CustomerCrupdatedService implements Consumer<CustomerCrupdated> {
  private static final String CUSTOMER_UPDATED_MAIL = "customer_updated_mail";
  private static final String CUSTOMER_CREATED_TEMPLATE_MAIL = "customer_created_template_mail";
  private final SesService service;
  private final CustomerRepository customerRepository;
  private final BanApi banApi;
  private final TemplateResolverEngine templateResolverEngine;

  @Override
  public void accept(CustomerCrupdated customerCrupdated) {
    String subject = customerCrupdated.getSubject();
    String recipient = customerCrupdated.getRecipientEmail();
    Customer customer = customerCrupdated.getCustomer();
    CustomerCrupdated.Type type = customerCrupdated.getType();
    List<Attachment> attachments = List.of();

    Customer updatedCustomer = customer.toBuilder().build();
    if (!updatedCustomer.getFullAddress().equals(updatedCustomer.getLatestFullAddress())
        || updatedCustomer.getLocation().getCoordinate() == null
        || (updatedCustomer.getLocation().getCoordinate().getLatitude() == null
            && updatedCustomer.getLocation().getCoordinate().getLongitude() == null)) {
      GeoPosition customerPosition = banApi.fSearch(customer.getAddress());
      if (customerPosition != null
          && !customerPosition.getCoordinates().equals(customer.getLocation().getCoordinate())) {
        updatedCustomer =
            customerRepository.save(
                customer.toBuilder()
                    .location(
                        Location.builder()
                            .coordinate(customerPosition.getCoordinates())
                            .address(customer.getAddress())
                            .longitude(customerPosition.getCoordinates().getLongitude())
                            .latitude(customerPosition.getCoordinates().getLatitude())
                            .build())
                    .build());
      }
    }

    var template =
        customerCrupdated.getType() == CREATE
            ? CUSTOMER_CREATED_TEMPLATE_MAIL
            : CUSTOMER_UPDATED_MAIL;
    String htmlBody =
        templateResolverEngine.parseTemplateResolver(
            template, configureCustomerContext(customerCrupdated.getUser(), updatedCustomer, type));
    try {
      service.sendEmail(recipient, null, subject, htmlBody, attachments);
      log.info("Email sent to notify {} update", updatedCustomer.describe());
    } catch (MessagingException | IOException e) {
      log.error("Email not sent : " + e.getMessage());
    }
  }

  private Context configureCustomerContext(
      User user, Customer customer, CustomerCrupdated.Type type) {
    Context context = new Context();
    context.setVariable("type", type.toString());
    context.setVariable("user", user);
    context.setVariable("customer", customer);
    return context;
  }
}
