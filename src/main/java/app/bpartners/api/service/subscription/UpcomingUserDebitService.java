package app.bpartners.api.service.subscription;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import app.bpartners.api.model.UnknownStripeCustomer;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.UnknownStripeCustomerJpaRepository;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.TemporalUtils;
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
  private final TemporalUtils temporalUtils;

  public UpcomingDebitedCustomers getUpcomingDebitedCustomers() {
    var enabledUsersBySubscriptionId =
        userService.getEnabledUsers().stream()
            .filter(user -> user.getUserSubscriptionId() != null)
            .collect(
                toMap(User::getUserSubscriptionId, identity(), (existing, duplicate) -> existing));

    var billedUsers = new ArrayList<User>();
    var notBilledStripeCustomers = new ArrayList<Customer>();

    for (Customer stripeCustomer : stripeCustomerService.getStripeCustomers()) {
      var hasUpcomingInvoice =
          stripeInvoiceService.getUpcomingStripeInvoice(stripeCustomer.getId()) != null;
      if (!hasUpcomingInvoice) {
        continue;
      }
      var matchedUser = enabledUsersBySubscriptionId.get(stripeCustomer.getId());
      if (matchedUser != null) {
        billedUsers.add(matchedUser);
      } else {
        notBilledStripeCustomers.add(stripeCustomer);
      }
    }

    saveUnknownCustomersFromStripe(notBilledStripeCustomers);

    return new UpcomingDebitedCustomers(billedUsers, notBilledStripeCustomers);
  }

  private void saveUnknownCustomersFromStripe(List<Customer> unknownStripeCustomers) {
    if (unknownStripeCustomers.isEmpty()) {
      return;
    }

    var alreadySavedIdentifiersThisMonth =
        unknownStripeCustomerJpaRepository
            .findAllByCreationDatetimeBetween(
                temporalUtils.startOfMonth(), temporalUtils.endOfMonth())
            .stream()
            .map(UnknownStripeCustomer::getStripeCustomerIdentifier)
            .collect(toSet());

    List<UnknownStripeCustomer> toSave =
        unknownStripeCustomers.stream()
            .filter(customer -> !alreadySavedIdentifiersThisMonth.contains(customer.getId()))
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

    if (toSave.isEmpty()) {
      return;
    }

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
