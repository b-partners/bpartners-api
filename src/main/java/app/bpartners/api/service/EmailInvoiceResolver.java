package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.EmailRecipientsUpdateRequested;
import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.accountholder.EmailRecipientService;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailInvoiceResolver implements Function<Invoice, String> {
  private final EmailRecipientService emailRecipientService;
  private final UserRepository userRepository;
  private final EventProducer eventProducer;

  public String apply(Invoice invoice) {
    Customer customer = invoice.getCustomer();
    if (customer == null || customer.getEmail() == null) {
      return customer == null ? null : customer.getEmail();
    }
    Optional<User> recipientUser = userRepository.findByEmail(customer.getEmail());
    if (recipientUser.isPresent()) {
      AccountHolder accountHolder = recipientUser.get().getDefaultHolder();
      if (accountHolder != null) {
        List<String> configuredEmails =
            emailRecipientService.getEmails(accountHolder.getId(), EmailRecipientType.INVOICE);
        if (!configuredEmails.isEmpty()) {
          var retainedEmailAddress = configuredEmails.getFirst();
          if (configuredEmails.size() > 1) {
            var ignoredEmailAddresses = configuredEmails.subList(1, configuredEmails.size());
            log.warn(
                "Only one email address supported for now but AccountHolder(id={}) has {}"
                    + " configured; {} retained, {} ignored",
                accountHolder.getId(),
                configuredEmails.size(),
                retainedEmailAddress,
                ignoredEmailAddresses);
          }
          return retainedEmailAddress;
        }
        requestInvoiceRecipientsUpdate(recipientUser.get(), accountHolder);
      }
    }
    return customer.getEmail();
  }

  private void requestInvoiceRecipientsUpdate(User recipientUser, AccountHolder accountHolder) {
    eventProducer.accept(
        List.of(
            EmailRecipientsUpdateRequested.builder()
                .userId(recipientUser.getId())
                .accountHolderId(accountHolder.getId())
                .type(EmailRecipientType.INVOICE)
                .build()));
    log.info(
        "No INVOICE email recipient for AccountHolder(id={}), requested async update from stripe"
            + " customer of User(id={})",
        accountHolder.getId(),
        recipientUser.getId());
  }
}
