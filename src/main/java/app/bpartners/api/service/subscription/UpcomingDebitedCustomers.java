package app.bpartners.api.service.subscription;

import app.bpartners.api.model.User;
import com.stripe.model.Customer;
import java.util.List;

public record UpcomingDebitedCustomers(List<User> billedUsers, List<Customer> notBilledCustomers) {}
