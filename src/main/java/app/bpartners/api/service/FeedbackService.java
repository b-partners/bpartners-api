package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.TypedFeedbackRequested;
import app.bpartners.api.endpoint.event.model.gen.FeedbackRequested;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Feedback;
import app.bpartners.api.repository.FeedBackRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FeedbackService {
  private final FeedBackRepository repository;
  private final EventProducer eventProducer;

  public Feedback save(String accountHolderId, Feedback toSave) {
    eventProducer.accept(List.of(toTypedEvent(toSave)));
    return repository.saveAll(accountHolderId, List.of(toSave)).get(0);
  }

  private TypedEvent toTypedEvent(Feedback feedback) {
    return new TypedFeedbackRequested(FeedbackRequested.builder()
        .subject(feedback.getSubject())
        .message(feedback.getMessage())
        .attachmentName(null)
        .recipientsEmails(getRecipientEmails(feedback.getCustomers()))
        .build());
  }

  private List<String> getRecipientEmails(List<Customer> recipients) {
    return recipients.stream()
        .map(Customer::getEmail)
        .collect(Collectors.toUnmodifiableList());

  }
}
