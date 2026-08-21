package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserStripeCustomerEmailCorrespondence;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.UserStripeCustomerEmailCorrespondenceJpaRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.customer.UserCustomerConverter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubscriptionCustomerResolverTest {
  CustomerRepository customerRepository = mock();
  UserCustomerConverter userCustomerConverter = mock();
  UserStripeCustomerEmailCorrespondenceJpaRepository correspondenceRepository = mock();
  SubscriptionCustomerResolver subject =
      new SubscriptionCustomerResolver(
          customerRepository, userCustomerConverter, correspondenceRepository);

  User admin = User.builder().id("admin_id").build();
  User buyer = User.builder().id("buyer_id").email("buyer@email.com").build();

  @Test
  void reuses_the_customer_matching_the_user_email() {
    var existing = Customer.builder().id("customer_id").build();
    givenCustomerFor("buyer@email.com", existing);

    var actual = subject.apply(admin, buyer);

    assertEquals(existing, actual);
    verify(correspondenceRepository, never()).findByUserId(any());
    verify(userCustomerConverter, never()).apply(any());
  }

  @Test
  void falls_back_on_the_customer_matching_the_stripe_email() {
    var existingFromStripeEmail = Customer.builder().id("stripe_customer_id").build();
    givenNoCustomerFor("buyer@email.com");
    givenStripeEmail("stripe@email.com");
    givenCustomerFor("stripe@email.com", existingFromStripeEmail);

    var actual = subject.apply(admin, buyer);

    assertEquals(existingFromStripeEmail, actual);
    verify(userCustomerConverter, never()).apply(any());
  }

  @Test
  void creates_the_customer_when_neither_email_matches() {
    var created = Customer.builder().id("created_customer_id").build();
    givenNoCustomerFor("buyer@email.com");
    givenStripeEmail("stripe@email.com");
    givenNoCustomerFor("stripe@email.com");
    when(userCustomerConverter.apply(buyer)).thenReturn(created);

    var actual = subject.apply(admin, buyer);

    assertEquals(created, actual);
  }

  @Test
  void creates_the_customer_when_the_user_has_no_stripe_email_correspondence() {
    var created = Customer.builder().id("created_customer_id").build();
    givenNoCustomerFor("buyer@email.com");
    when(correspondenceRepository.findByUserId("buyer_id")).thenReturn(Optional.empty());
    when(userCustomerConverter.apply(buyer)).thenReturn(created);

    var actual = subject.apply(admin, buyer);

    assertEquals(created, actual);
  }

  private void givenStripeEmail(String email) {
    when(correspondenceRepository.findByUserId("buyer_id"))
        .thenReturn(
            Optional.of(UserStripeCustomerEmailCorrespondence.builder().email(email).build()));
  }

  private void givenCustomerFor(String email, Customer customer) {
    when(customerRepository.findByIdUserAndCriteria(
            eq("admin_id"),
            any(),
            any(),
            eq(email),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(CustomerStatus.ENABLED),
            anyInt(),
            anyInt()))
        .thenReturn(List.of(customer));
  }

  private void givenNoCustomerFor(String email) {
    when(customerRepository.findByIdUserAndCriteria(
            eq("admin_id"),
            any(),
            any(),
            eq(email),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(CustomerStatus.ENABLED),
            anyInt(),
            anyInt()))
        .thenReturn(List.of());
  }
}
