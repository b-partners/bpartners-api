package app.bpartners.api.endpoint.rest.mapper;

import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.CreatedFeedbackRequest;
import app.bpartners.api.endpoint.rest.model.FeedbackRequest;
import app.bpartners.api.endpoint.rest.validator.FeedBackRestValidator;
import app.bpartners.api.model.Feedback;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FeedbackRestMapper {
  private final CustomerRestMapper customerRestMapper;
  private final FeedBackRestValidator validator;

  public app.bpartners.api.model.FeedbackRequest toDomain(
      String accountHolderId, FeedbackRequest rest) {
    validator.accept(rest);
    return app.bpartners.api.model.FeedbackRequest.builder()
        .id(String.valueOf(randomUUID()))
        .accountHolderId(accountHolderId)
        .subject(rest.getSubject())
        .message(rest.getMessage())
        .customerIds(rest.getCustomerIds())
        .creationDatetime(Instant.now())
        .build();
  }

  public CreatedFeedbackRequest toRest(Feedback domain) {
    return new CreatedFeedbackRequest()
        .id(domain.getId())
        .customers(domain.getCustomers().stream().map(customerRestMapper::toRest).toList())
        .creationDatetime(domain.getCreationDatetime());
  }
}
