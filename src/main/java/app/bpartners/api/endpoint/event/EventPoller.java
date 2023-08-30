package app.bpartners.api.endpoint.event;

import app.bpartners.api.endpoint.event.EventConsumer.AcknowledgeableTypedEvent;
import app.bpartners.api.endpoint.event.model.TypedCustomerCrupdated;
import app.bpartners.api.endpoint.event.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.TypedFeedbackRequested;
import app.bpartners.api.endpoint.event.model.TypedInvoiceCrupdated;
import app.bpartners.api.endpoint.event.model.TypedInvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.model.TypedUserOnboarded;
import app.bpartners.api.endpoint.event.model.TypedUserUpserted;
import app.bpartners.api.endpoint.event.model.gen.CustomerCrupdated;
import app.bpartners.api.endpoint.event.model.gen.FeedbackRequested;
import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.model.gen.UserOnboarded;
import app.bpartners.api.endpoint.event.model.gen.UserUpserted;
import app.bpartners.api.model.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
@Slf4j
public class EventPoller {

  private static final int MAX_NUMBER_OF_MESSAGES = 10;
  private static final Duration WAIT_TIME = Duration.ofSeconds(0); // MUST be if long-polling <= 20s
  public static final String DETAIL_PROPERTY = "detail";
  private final String queueUrl;
  private final SqsClient sqsClient;
  private final ObjectMapper om;
  private final EventConsumer eventConsumer;

  public EventPoller(
      @Value("${aws.sqs.mailboxUrl}") String queueUrl,
      SqsClient sqsClient,
      ObjectMapper om,
      EventConsumer eventConsumer) {
    this.queueUrl = queueUrl;
    this.sqsClient = sqsClient;
    this.om = om;
    this.eventConsumer = eventConsumer;
  }

  @Scheduled(fixedRate = 200, initialDelay = 120000)
  public void poll() {
    ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
        .queueUrl(queueUrl)
        .waitTimeSeconds(WAIT_TIME.toSecondsPart())
        .maxNumberOfMessages(MAX_NUMBER_OF_MESSAGES)
        .build();

    List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
    if (!messages.isEmpty()) {
      log.info("Events received: {}", messages);
      var ackEvents = toAcknowledgeableTypedEvents(messages);
      eventConsumer.accept(ackEvents);
    }
  }

  private List<AcknowledgeableTypedEvent> toAcknowledgeableTypedEvents(List<Message> messages) {
    List<AcknowledgeableTypedEvent> res = new ArrayList<>();

    for (Message message : messages) {
      TypedEvent typedEvent;
      try {
        typedEvent = toTypedEvent(message);
      } catch (Exception e) {
        log.error("Message could not be unmarshalled, message={}", message, e);
        continue;
      }

      res.add(new AcknowledgeableTypedEvent(
          typedEvent,
          () -> sqsClient.deleteMessage(DeleteMessageRequest.builder()
              .queueUrl(queueUrl)
              .receiptHandle(message.receiptHandle())
              .build())));
    }

    return res;
  }

  private TypedEvent toTypedEvent(Message message) throws JsonProcessingException {
    TypedEvent typedEvent;

    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
    };
    Map<String, Object> body = om.readValue(message.body(), typeRef);
    String typeName = body.get("detail-type").toString();
    if (InvoiceRelaunchSaved.class.getTypeName().equals(typeName)) {
      InvoiceRelaunchSaved
          invoiceRelaunchSaved =
          om.convertValue(body.get(DETAIL_PROPERTY), InvoiceRelaunchSaved.class);
      typedEvent = new TypedInvoiceRelaunchSaved(invoiceRelaunchSaved);
    } else if (InvoiceCrupdated.class.getTypeName().equals(typeName)) {
      InvoiceCrupdated invoiceCrupdated =
          om.convertValue(body.get(DETAIL_PROPERTY), InvoiceCrupdated.class);
      typedEvent = new TypedInvoiceCrupdated(invoiceCrupdated);
    } else if (UserUpserted.class.getTypeName().equals(typeName)) {
      UserUpserted userUpserted =
          om.convertValue(body.get(DETAIL_PROPERTY), UserUpserted.class);
      typedEvent = new TypedUserUpserted(userUpserted);
    } else if (FeedbackRequested.class.getTypeName().equals(typeName)) {
      FeedbackRequested feedbackRequested =
          om.convertValue(body.get(DETAIL_PROPERTY), FeedbackRequested.class);
      typedEvent = new TypedFeedbackRequested(feedbackRequested);
    } else if (CustomerCrupdated.class.getTypeName().equals(typeName)) {
      CustomerCrupdated customerCrupdated =
          om.convertValue(body.get(DETAIL_PROPERTY), CustomerCrupdated.class);
      typedEvent = new TypedCustomerCrupdated(customerCrupdated);
    } else if (UserOnboarded.class.getTypeName().equals(typeName)) {
      UserOnboarded userOnboarded =
          om.convertValue(body.get(DETAIL_PROPERTY), UserOnboarded.class);
      typedEvent = new TypedUserOnboarded(userOnboarded);
    } else {
      throw new BadRequestException("Unexpected message type for message=" + message);
    }

    return typedEvent;
  }
}
