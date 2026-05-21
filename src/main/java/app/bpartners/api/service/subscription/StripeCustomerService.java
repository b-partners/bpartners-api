package app.bpartners.api.service.subscription;

import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import com.stripe.StripeClient;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.param.CustomerListParams;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeCustomerService {
  private final StripeClient stripeClient;

  @SneakyThrows
  public @NotNull Customer getCustomerByStripeCustomerIdentifier(String stripeCustomerId) {
    if (stripeCustomerId == null) {
      throw new IllegalArgumentException("Stripe customer id is mandatory and can not be null");
    }
    return stripeClient.customers().retrieve(stripeCustomerId);
  }

  public Customer getCustomer(User user) {
    try {
      return getCustomerByStripeCustomerIdentifier(user.getUserSubscriptionId());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "User.id=" + user.getId() + " is not associated to a stripe customer yet");
    }
  }

  @SneakyThrows
  public List<Customer> getStripeCustomers() {
    CustomerListParams params = CustomerListParams.builder().setLimit(100L).build();

    CustomerCollection customers = Customer.list(params);

    List<Customer> aggCustomers = new ArrayList<>();
    for (Customer customer : customers.autoPagingIterable()) {
      aggCustomers.add(customer);
    }

    return aggCustomers;
  }
}
