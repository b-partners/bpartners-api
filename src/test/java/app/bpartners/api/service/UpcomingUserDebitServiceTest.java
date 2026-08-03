package app.bpartners.api.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.UnknownStripeCustomer;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.UnknownStripeCustomerJpaRepository;
import app.bpartners.api.service.subscription.StripeCustomerService;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.UpcomingUserDebitService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.model.Address;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpcomingUserDebitServiceTest {
  StripeInvoiceService stripeInvoiceServiceMock = mock();
  StripeCustomerService stripeCustomerServiceMock = mock();
  UserService userServiceMock = mock();
  UnknownStripeCustomerJpaRepository unknownStripeCustomerJpaRepositoryMock = mock();
  UpcomingUserDebitService subject =
      new UpcomingUserDebitService(
          stripeInvoiceServiceMock,
          stripeCustomerServiceMock,
          userServiceMock,
          unknownStripeCustomerJpaRepositoryMock,
          new TemporalUtils());

  @Test
  void get_upcoming_user_debited_without_any_unknown_customers() {
    var userMock = mock(User.class);
    var stripeCustomerMock = mock(Customer.class);
    var stripeCustomerIdentifier = randomUUID().toString();
    when(userMock.getUserSubscriptionId()).thenReturn(stripeCustomerIdentifier);
    when(stripeCustomerMock.getId()).thenReturn(stripeCustomerIdentifier);
    when(userServiceMock.getEnabledUsers()).thenReturn(new ArrayList<>(List.of(userMock)));
    when(stripeCustomerServiceMock.getStripeCustomers()).thenReturn(List.of(stripeCustomerMock));
    when(stripeInvoiceServiceMock.getUpcomingStripeInvoice(stripeCustomerIdentifier))
        .thenReturn(mock(Invoice.class));

    var actual = subject.getUpcomingDebitedCustomers();

    assertEquals(List.of(userMock), actual.billedUsers());
    assertEquals(List.of(), actual.notBilledCustomers());
    verify(unknownStripeCustomerJpaRepositoryMock, never()).saveAll(any());
  }

  @Test
  void get_upcoming_user_debited_with_unknown_customers() {
    var userMock = mock(User.class);
    var stripeCustomerMock = mock(Customer.class);
    var unknownStripeCustomerMock = mock(Customer.class);
    var stripeCustomerIdentifier = randomUUID().toString();
    var unknownStripeCustomerIdentifier = randomUUID().toString();
    var addressMock = mock(Address.class);
    when(userMock.getUserSubscriptionId()).thenReturn(stripeCustomerIdentifier);
    when(stripeCustomerMock.getId()).thenReturn(stripeCustomerIdentifier);
    when(addressMock.getLine1()).thenReturn("123 Main St");
    when(addressMock.getCity()).thenReturn("Anytown");
    when(addressMock.getPostalCode()).thenReturn("12345");
    when(addressMock.getCountry()).thenReturn("US");
    when(unknownStripeCustomerMock.getId()).thenReturn(unknownStripeCustomerIdentifier);
    when(unknownStripeCustomerMock.getName()).thenReturn("unknown customer name");
    when(unknownStripeCustomerMock.getEmail()).thenReturn("unknown@example.com");
    when(unknownStripeCustomerMock.getPhone()).thenReturn("0234567890");
    when(unknownStripeCustomerMock.getAddress()).thenReturn(addressMock);
    when(userServiceMock.getEnabledUsers()).thenReturn(new ArrayList<>(List.of(userMock)));
    when(stripeCustomerServiceMock.getStripeCustomers())
        .thenReturn(List.of(stripeCustomerMock, unknownStripeCustomerMock));
    when(stripeInvoiceServiceMock.getUpcomingStripeInvoice(stripeCustomerIdentifier))
        .thenReturn(mock(Invoice.class));
    when(stripeInvoiceServiceMock.getUpcomingStripeInvoice(unknownStripeCustomerIdentifier))
        .thenReturn(mock(Invoice.class));

    var actual = subject.getUpcomingDebitedCustomers();

    assertEquals(List.of(userMock), actual.billedUsers());
    assertEquals(List.of(unknownStripeCustomerMock), actual.notBilledCustomers());
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(unknownStripeCustomerJpaRepositoryMock).saveAll(listCaptor.capture());
    var unknownStripeCustomer = (UnknownStripeCustomer) listCaptor.getValue().getFirst();
    assertEquals(
        UnknownStripeCustomer.builder()
            .id(unknownStripeCustomer.getId())
            .stripeCustomerIdentifier(unknownStripeCustomerIdentifier)
            .name("unknown customer name")
            .email("unknown@example.com")
            .phone("0234567890")
            .address("123 Main St  Anytown 12345 US")
            .creationDatetime(unknownStripeCustomer.getCreationDatetime())
            .build(),
        unknownStripeCustomer);
  }

  @Test
  void does_not_resave_unknown_customer_already_persisted_this_month() {
    var userMock = mock(User.class);
    var stripeCustomerMock = mock(Customer.class);
    var unknownStripeCustomerMock = mock(Customer.class);
    var stripeCustomerIdentifier = randomUUID().toString();
    var unknownStripeCustomerIdentifier = randomUUID().toString();
    when(userMock.getUserSubscriptionId()).thenReturn(stripeCustomerIdentifier);
    when(stripeCustomerMock.getId()).thenReturn(stripeCustomerIdentifier);
    when(unknownStripeCustomerMock.getId()).thenReturn(unknownStripeCustomerIdentifier);
    when(userServiceMock.getEnabledUsers()).thenReturn(new ArrayList<>(List.of(userMock)));
    when(stripeCustomerServiceMock.getStripeCustomers())
        .thenReturn(List.of(stripeCustomerMock, unknownStripeCustomerMock));
    when(stripeInvoiceServiceMock.getUpcomingStripeInvoice(stripeCustomerIdentifier))
        .thenReturn(mock(Invoice.class));
    when(stripeInvoiceServiceMock.getUpcomingStripeInvoice(unknownStripeCustomerIdentifier))
        .thenReturn(mock(Invoice.class));
    when(unknownStripeCustomerJpaRepositoryMock.findAllByCreationDatetimeBetween(any(), any()))
        .thenReturn(
            List.of(
                UnknownStripeCustomer.builder()
                    .id(randomUUID().toString())
                    .stripeCustomerIdentifier(unknownStripeCustomerIdentifier)
                    .build()));

    var actual = subject.getUpcomingDebitedCustomers();

    assertEquals(List.of(userMock), actual.billedUsers());
    assertEquals(List.of(unknownStripeCustomerMock), actual.notBilledCustomers());
    verify(unknownStripeCustomerJpaRepositoryMock, never()).saveAll(any());
  }
}
