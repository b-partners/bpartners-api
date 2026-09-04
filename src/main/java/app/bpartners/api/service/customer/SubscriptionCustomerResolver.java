package app.bpartners.api.service.customer;

import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.UserStripeCustomerEmailCorrespondenceJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionCustomerResolver {
  private final CustomerRepository customerRepository;
  private final UserCustomerConverter userCustomerConverter;
  private final UserStripeCustomerEmailCorrespondenceJpaRepository
      userStripeCustomerEmailCorrespondenceJpaRepository;

  public Customer apply(User userToCredit, User userToDebit) {
    var optionalCustomerToDebitFromOriginalUserToDebitEmail =
        findByEmail(userToCredit, userToDebit.getEmail());
    if (optionalCustomerToDebitFromOriginalUserToDebitEmail.isEmpty()) {
      var optionalUserStripeCustomerEmailCorrespondence =
          userStripeCustomerEmailCorrespondenceJpaRepository.findByUserId(userToDebit.getId());
      if (optionalUserStripeCustomerEmailCorrespondence.isPresent()) {
        return findByEmail(
                userToCredit, optionalUserStripeCustomerEmailCorrespondence.get().getEmail())
            .orElseGet(() -> userCustomerConverter.apply(userToDebit));
      }
    }
    return optionalCustomerToDebitFromOriginalUserToDebitEmail.orElseGet(
        () -> userCustomerConverter.apply(userToDebit));
  }

  private Optional<Customer> findByEmail(User userToCredit, String email) {
    return customerRepository
        .findByIdUserAndCriteria(
            userToCredit.getId(),
            null,
            null,
            email,
            null,
            null,
            null,
            null,
            null,
            CustomerStatus.ENABLED,
            MIN_PAGE,
            MAX_SIZE)
        .stream()
        .findAny();
  }
}
