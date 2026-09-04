package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.EmailRecipientsUpdateRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.accountholder.EmailRecipientService;
import app.bpartners.api.service.subscription.StripeCustomerService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailRecipientsUpdateRequestedService
    implements Consumer<EmailRecipientsUpdateRequested> {
  private final UserRepository userRepository;
  private final StripeCustomerService stripeCustomerService;
  private final EmailRecipientService emailRecipientService;

  @Override
  public void accept(EmailRecipientsUpdateRequested event) {
    var accountHolderId = event.getAccountHolderId();
    var type = event.getType();
    if (!emailRecipientService.getEmails(accountHolderId, type).isEmpty()) {
      log.info(
          "AccountHolder(id={}) already has {} email recipients, skipping update",
          accountHolderId,
          type);
      return;
    }

    User user = userRepository.getById(event.getUserId());
    String stripeCustomerEmail = stripeCustomerService.getCustomer(user).getEmail();
    if (stripeCustomerEmail == null) {
      log.warn(
          "Stripe customer of User(id={}) has no email, {} email recipients not updated",
          user.getId(),
          type);
      return;
    }

    emailRecipientService.populateByType(accountHolderId, type, List.of(stripeCustomerEmail));
    log.info(
        "AccountHolder(id={}) {} email recipients populated with stripe customer email of"
            + " User(id={})",
        accountHolderId,
        type,
        user.getId());
  }
}
