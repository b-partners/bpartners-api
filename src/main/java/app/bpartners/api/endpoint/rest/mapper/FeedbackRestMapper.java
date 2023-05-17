package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatedFeedbackRequest;
import app.bpartners.api.endpoint.rest.model.FeedbackRequest;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Feedback;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.CustomerRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class FeedbackRestMapper {
  private final CustomerRepository customerRepository;
  private final AccountHolderRepository accountHolderRepository;
  private final CustomerRestMapper customerRestMapper;

  public Feedback toDomain(String accountHolderId, FeedbackRequest rest) {
    AccountHolder accountHolder = accountHolderRepository.findById(accountHolderId);
    List<Customer> customers = rest.getCustomerIds() != null ? rest.getCustomerIds().stream()
        .map(customerRepository::findById)
        .collect(Collectors.toUnmodifiableList()) : null;
    return Feedback.builder()
        .id(String.valueOf(randomUUID()))
        .accountHolder(accountHolder)
        .customers(customers)
        .creationDatetime(Instant.now())
        .build();
  }

  public CreatedFeedbackRequest toRest(Feedback domain) {
    return new CreatedFeedbackRequest()
        .id(domain.getId())
        .customers(domain.getCustomers().stream()
            .map(customerRestMapper::toRest)
            .collect(Collectors.toUnmodifiableList()))
        .creationDatetime(domain.getCreationDatetime());
  }
}
