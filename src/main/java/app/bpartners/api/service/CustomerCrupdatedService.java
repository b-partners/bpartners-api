package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.CustomerCrupdated;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.User;
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

  @Override
  public void accept(CustomerCrupdated customerCrupdated) {
    String subject = customerCrupdated.getSubject();
    String recipient = customerCrupdated.getRecipientEmail();
    Customer customer = customerCrupdated.getCustomer();
    CustomerCrupdated.Type type = customerCrupdated.getType();
    List<Attachment> attachments = List.of();

    String
        htmlBody = parseTemplateResolver(CUSTOMER_CRUPDATED_MAIL,
        configureCustomerContext(customerCrupdated.getUser(), customer,
            type));


    try {
      service.sendEmail(recipient, subject, htmlBody, attachments);
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
