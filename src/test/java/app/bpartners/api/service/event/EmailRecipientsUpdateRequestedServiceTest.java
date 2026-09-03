package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.EmailRecipientsUpdateRequested;
import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.accountholder.EmailRecipientService;
import app.bpartners.api.service.subscription.StripeCustomerService;
import com.stripe.model.Customer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailRecipientsUpdateRequestedServiceTest {
  UserRepository userRepository = mock();
  StripeCustomerService stripeCustomerService = mock();
  EmailRecipientService emailRecipientService = mock();
  EmailRecipientsUpdateRequestedService subject =
      new EmailRecipientsUpdateRequestedService(
          userRepository, stripeCustomerService, emailRecipientService);

  private EmailRecipientsUpdateRequested someEvent() {
    return EmailRecipientsUpdateRequested.builder()
        .userId("user_id")
        .accountHolderId("account_holder_id")
        .type(EmailRecipientType.INVOICE)
        .build();
  }

  @Test
  void populates_recipients_from_stripe_customer_email() {
    when(emailRecipientService.getEmails("account_holder_id", EmailRecipientType.INVOICE))
        .thenReturn(List.of());
    var user = User.builder().id("user_id").build();
    when(userRepository.getById("user_id")).thenReturn(user);
    var stripeCustomer = mock(Customer.class);
    when(stripeCustomer.getEmail()).thenReturn("compta@client.fr");
    when(stripeCustomerService.getCustomer(user)).thenReturn(stripeCustomer);

    subject.accept(someEvent());

    var emailsCaptor = ArgumentCaptor.forClass(List.class);
    verify(emailRecipientService)
        .populateByType(
            eq("account_holder_id"), eq(EmailRecipientType.INVOICE), emailsCaptor.capture());
    assertEquals(List.of("compta@client.fr"), emailsCaptor.getValue());
  }

  @Test
  void skips_when_recipients_already_configured() {
    when(emailRecipientService.getEmails("account_holder_id", EmailRecipientType.INVOICE))
        .thenReturn(List.of("already@client.fr"));

    subject.accept(someEvent());

    verify(userRepository, never()).getById(anyString());
    verify(emailRecipientService, never()).populateByType(anyString(), any(), anyList());
  }

  @Test
  void skips_when_stripe_customer_has_no_email() {
    when(emailRecipientService.getEmails("account_holder_id", EmailRecipientType.INVOICE))
        .thenReturn(List.of());
    var user = User.builder().id("user_id").build();
    when(userRepository.getById("user_id")).thenReturn(user);
    var stripeCustomer = mock(Customer.class);
    when(stripeCustomer.getEmail()).thenReturn(null);
    when(stripeCustomerService.getCustomer(user)).thenReturn(stripeCustomer);

    subject.accept(someEvent());

    verify(emailRecipientService, never()).populateByType(anyString(), any(), anyList());
  }
}
