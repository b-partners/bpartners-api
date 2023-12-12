package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.gen.FeedbackRequested;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Feedback;
import app.bpartners.api.model.FeedbackRequest;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.FeedBackRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FeedbackService {
  private final FeedBackRepository repository;
  private final EventProducer eventProducer;
  private final CustomerRepository customerRepository;
  private final AccountHolderRepository accountHolderRepository;

  public Feedback save(FeedbackRequest toSave) {
    AccountHolder accountHolder = accountHolderRepository.findById(toSave.getAccountHolderId());
    if (accountHolder.getFeedbackLink() == null) {
      throw new BadRequestException(
          accountHolder.describe() + " must set feedback link before "
              + "asking for feedback");
    }
    List<Customer> customers = toSave.getCustomerIds() != null ? toSave.getCustomerIds().stream()
        .map(customerRepository::findById)
        .toList() : null;
    Feedback toCreate = Feedback.builder()
        .id(toSave.getId())
        .accountHolder(accountHolder)
        .customers(customers)
        .creationDatetime(toSave.getCreationDatetime())
        .subject(toSave.getSubject())
        .message(toSave.getMessage())
        .build();
    eventProducer.accept(List.of(toTypedEvent(toCreate)));
    return repository.save(toCreate);
  }

  private FeedbackRequested toTypedEvent(Feedback feedback) {
    return FeedbackRequested.builder()
        .subject(feedback.getSubject())
        .message(feedback.getMessage())
        .attachmentName(null)
        .recipientsEmails(getRecipientEmails(feedback.getCustomers()))
        .build();
  }

  private List<String> getRecipientEmails(List<Customer> recipients) {
    List<Customer> customerListWithEmail = recipients.stream()
        .filter(recipient -> recipient.getEmail() != null)
        .toList();
    return customerListWithEmail.stream()
        .map(Customer::getEmail)
        .toList();
  }
}