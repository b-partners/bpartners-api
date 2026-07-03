package app.bpartners.api.service.subscription;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import app.bpartners.api.model.UnknownStripeCustomer;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.UnknownStripeCustomerJpaRepository;
import app.bpartners.api.service.user.UserService;
import com.stripe.model.Customer;
import java.util.ArrayList;
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
    var enabledUsersBySubscriptionId =
        userService.getEnabledUsers().stream()
            .filter(user -> user.getUserSubscriptionId() != null)
            .collect(
                toMap(User::getUserSubscriptionId, identity(), (existing, duplicate) -> existing));

    var upcomingUsersToDebit = new ArrayList<User>();
    var unknownCustomers = new ArrayList<Customer>();

    for (Customer stripeCustomer : stripeCustomerService.getStripeCustomers()) {
      var hasUpcomingInvoice =
          stripeInvoiceService.getUpcomingStripeInvoice(stripeCustomer.getId()) != null;
      if (!hasUpcomingInvoice) {
        continue;
      }
      var matchedUser = enabledUsersBySubscriptionId.get(stripeCustomer.getId());
      if (matchedUser != null) {
        upcomingUsersToDebit.add(matchedUser);
      } else {
        unknownCustomers.add(stripeCustomer);
      }
    }

    saveUnknownCustomersFromStripe(unknownCustomers);

    return upcomingUsersToDebit;
  }

  private void saveUnknownCustomersFromStripe(List<Customer> unknownStripeCustomers) {
    if (unknownStripeCustomers.isEmpty()) {
      return;
    }

    List<UnknownStripeCustomer> toSave =
        unknownStripeCustomers.stream()
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

    unknownStripeCustomerJpaRepository.saveAll(toSave);
  }

  private String computeFullTextAddressFromStripeCustomer(Customer customer) {
    var customerAddress = customer.getAddress();
    if (customerAddress == null) {
      return null;
    }
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
