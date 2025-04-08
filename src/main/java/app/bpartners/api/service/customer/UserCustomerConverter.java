package app.bpartners.api.service.customer;

import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.CustomerType;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.UserRepository;
import java.time.Instant;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCustomerConverter implements Function<User, Customer> {
  private final String ADMIN_RECIPIENT = System.getenv("ADMIN.EMAIL");
  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;

  @Override
  public Customer apply(User user) {
    var accountHolderToDebit = user.getDefaultHolder();
    return customerRepository.save(
        Customer.builder()
            .id(randomUUID().toString())
            .idUser(getAdminUserId())
            .name(accountHolderToDebit.getName())
            .firstName(user.getFirstName()) // TODO: Bad ! because customers are company
            .lastName(user.getLastName()) // TODO: Bad ! because customers are company
            .email(user.getEmail())
            .phone(accountHolderToDebit.getMobilePhoneNumber())
            .website(accountHolderToDebit.getWebsite())
            .address(accountHolderToDebit.getAddress())
            .zipCode(
                accountHolderToDebit.getPostalCode() == null
                    ? null
                    : Integer.valueOf(accountHolderToDebit.getPostalCode()))
            .city(accountHolderToDebit.getCity())
            .country(accountHolderToDebit.getCountry())
            .comment(null)
            .location(null) // TODO: compute location ?
            .status(CustomerStatus.ENABLED)
            .customerType(CustomerType.PROFESSIONAL)
            .recentlyAdded(false)
            .updatedAt(Instant.now())
            .createdAt(Instant.now())
            .build());
  }

  private @Nullable String getAdminUserId() {
    var user = userRepository.findByEmail(ADMIN_RECIPIENT).orElse(null);
    return user == null ? null : user.getId();
  }
}
