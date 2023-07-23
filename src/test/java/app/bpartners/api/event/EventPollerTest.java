package app.bpartners.api.event;

import app.bpartners.api.endpoint.event.EventConsumer;
import app.bpartners.api.endpoint.event.EventPoller;
import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.endpoint.rest.model.FileType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static app.bpartners.api.integration.conf.utils.TestUtils.FILE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventPollerTest {
  EventPoller eventPoller;
  SqsClient sqsClient;
  EventConsumer eventConsumer;

  @BeforeEach
  void setUp() {
    sqsClient = mock(SqsClient.class);
    eventConsumer = mock(EventConsumer.class);
    eventPoller = new EventPoller(
        "queueUrl",
        sqsClient,
        new ObjectMapper(),
        eventConsumer);
  }

  @Test
  void empty_messages_not_triggers_eventConsumer() {
    ReceiveMessageResponse response = ReceiveMessageResponse.builder()
        .messages(List.of())
        .build();
    when(sqsClient.receiveMessage((ReceiveMessageRequest) any())).thenReturn(response);

    eventPoller.poll();

    verify(eventConsumer, never()).accept(any());
  }

  @Test
  void non_empty_messages_triggers_eventConsumer() {
    ReceiveMessageResponse response = ReceiveMessageResponse.builder()
        .messages(
            someMessage(Exception.class),
            someMessage(InvoiceRelaunchSaved.class))
        .build();
    when(sqsClient.receiveMessage((ReceiveMessageRequest) any())).thenReturn(response);

    eventPoller.poll();

    ArgumentCaptor<List<EventConsumer.AcknowledgeableTypedEvent>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventConsumer, times(1)).accept(captor.capture());
    var ackTypedEvents = captor.getValue();
    assertEquals(1, ackTypedEvents.size());
    // Second ackTypedEvent
    var ackTypedEvent = ackTypedEvents.get(0);
    var typeEvent = ackTypedEvent.getTypedEvent();
    assertEquals(InvoiceRelaunchSaved.class.getTypeName(), typeEvent.getTypeName());
    InvoiceRelaunchSaved invoiceRelaunchSaved = (InvoiceRelaunchSaved) typeEvent.getPayload();
    assertFalse(invoiceRelaunchSaved.getSubject().isEmpty());
    assertFalse(invoiceRelaunchSaved.getRecipient().isEmpty());
    assertFalse(invoiceRelaunchSaved.getAttachmentName().isEmpty());
    assertFalse(invoiceRelaunchSaved.getHtmlBody().isEmpty());
  }

  private Message someMessage(Class<?> clazz) {
    return Message.builder()
        .body(messageBody(clazz))
        .receiptHandle(randomUUID().toString())
        .build();
  }

  private String messageBody(Class<?> clazz) {
    String eventId = randomUUID().toString();
    if (clazz.getTypeName().equals(FileSaved.class.getTypeName())) {
      return "{\n"
          + "    \"version\": \"0\",\n"
          + "    \"id\": \" " + eventId + "\",\n"
          + "    \"detail-type\": \"" + clazz.getTypeName() + "\",\n"
          + "    \"source\": \"app.bpartners.api\",\n"
          + "    \"account\": \"088312068315\",\n"
          + "    \"time\": \"" + now() + "\",\n"
          + "    \"region\": \"eu-west-3\",\n"
          + "    \"resources\": [],\n"
          + "    \"detail\": {\n"
          + "        \"fileType\": \"" + FileType.INVOICE + "\",\n"
          + "        \"accountId\": \"" + JOE_DOE_ACCOUNT_ID + "\",\n"
          + "        \"fileId\": \"" + FILE_ID + "\",\n"
          + "        \"fileAsBytes\": []\n"
          + "    }\n"
          + "}";
    }
    String userId = randomUUID().toString();
    return "{\n"
        + "    \"version\": \"0\",\n"
        + "    \"id\": \" " + eventId + "\",\n"
        + "    \"detail-type\": \"" + clazz.getTypeName() + "\",\n"
        + "    \"source\": \"app.bpartners.api\",\n"
        + "    \"account\": \"088312068315\",\n"
        + "    \"time\": \"" + now() + "\",\n"
        + "    \"region\": \"eu-west-3\",\n"
        + "    \"resources\": [],\n"
        + "    \"detail\": {\n"
        + "        \"recipient\": \"test+" + userId + "@bpartners.app\",\n"
        + "        \"subject\": \"Objet du mail\",\n"
        + "        \"invoice\": {},\n"
        + "        \"accountHolder\": {},\n"
        + "        \"logoFileId\": \"\",\n"
        + "        \"attachmentName\": \"Nom de la pièce jointe\",\n"
        + "        \"htmlBody\": \"<html><body>Corps du mail</body></html>\"\n"
        + "    }\n"
        + "}";
  }
}