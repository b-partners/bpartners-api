package app.bpartners.api.service.subscription;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.UnknownStripeCustomer;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.UnknownStripeCustomerJpaRepository;
import app.bpartners.api.service.user.UserService;
import com.stripe.model.Customer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpcomingUserDebitService {
  private final StripeInvoiceService stripeInvoiceService;
  private final StripeCustomerService stripeCustomerService;
  private final UserService userService;
  private final UnknownStripeCustomerJpaRepository unknownStripeCustomerJpaRepository;

  public List<User> getUpcomingUserDebited() {
    var enabledUsers = userService.getEnabledUsers();

    var stripeCustomers = stripeCustomerService.getStripeCustomers();

    var stripeCustomersWithUpcomingInvoices =
        stripeCustomers.stream()
            .filter(
                stripeCustomer ->
                    stripeInvoiceService.getUpcomingStripeInvoice(stripeCustomer.getId()) != null)
            .toList();

    var userWithUpcomingInvoiceFilteredWithStripeCustomers =
        enabledUsers.stream()
            .filter(
                user ->
                    user.getUserSubscriptionId() != null
                        && stripeCustomersWithUpcomingInvoices.stream()
                            .anyMatch(
                                stripeCustomer ->
                                    stripeCustomer.getId().equals(user.getUserSubscriptionId())))
            .toList();

    saveUnknownCustomersFromStripe(
        stripeCustomersWithUpcomingInvoices, userWithUpcomingInvoiceFilteredWithStripeCustomers);

    return userWithUpcomingInvoiceFilteredWithStripeCustomers;
  }

  private void saveUnknownCustomersFromStripe(
      List<Customer> stripeCustomersWithUpcomingInvoices,
      List<User> userWithUpcomingInvoiceFilteredWithStripeCustomers) {
    var stripeCustomersWithUpcomingInvoicesNotIdentified =
        stripeCustomersWithUpcomingInvoices.stream()
            .filter(
                stripeCustomer ->
                    userWithUpcomingInvoiceFilteredWithStripeCustomers.stream()
                        .noneMatch(
                            user -> user.getUserSubscriptionId().equals(stripeCustomer.getId())))
            .toList();

    List<UnknownStripeCustomer> unknownStripeCustomers =
        stripeCustomersWithUpcomingInvoicesNotIdentified.stream()
            .map(
                customer ->
                    UnknownStripeCustomer.builder()
                        .id(randomUUID().toString())
                        .stripeCustomerIdentifier(customer.getId())
                        .name(customer.getName())
                        .email(customer.getEmail())
                        .phone(customer.getPhone())
                        .address(computeFullTextAddressFromStripeCustomer(customer))
                        .creationDatetime(now())
                        .build())
            .toList();

    if (!unknownStripeCustomers.isEmpty()) {
      unknownStripeCustomerJpaRepository.saveAll(unknownStripeCustomers);
    }
  }

  private String computeFullTextAddressFromStripeCustomer(Customer customer) {
    var customerAddress = customer.getAddress();
    return concatIfPresent(customerAddress.getLine1())
        + " "
        + concatIfPresent(customerAddress.getLine2())
        + " "
        + concatIfPresent(customerAddress.getCity())
        + " "
        + concatIfPresent(customerAddress.getPostalCode())
        + " "
        + concatIfPresent(customerAddress.getCountry());
  }

  private String concatIfPresent(String v) {
    return v == null ? "" : v;
  }
}
