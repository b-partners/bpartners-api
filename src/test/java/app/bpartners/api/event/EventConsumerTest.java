package app.bpartners.api.event;

import app.bpartners.api.endpoint.event.EventConsumer;
import app.bpartners.api.endpoint.event.EventServiceInvoker;
import app.bpartners.api.endpoint.event.model.TypedFileSaved;
import app.bpartners.api.endpoint.event.model.TypedMailSent;
import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import app.bpartners.api.endpoint.event.model.gen.MailSent;
import app.bpartners.api.endpoint.rest.model.FileType;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class EventConsumerTest {
  EventConsumer eventConsumer;
  EventServiceInvoker eventServiceInvoker;

  static final Duration TIMEOUT = Duration.ofSeconds(3);

  @BeforeEach
  void setUp() {
    eventServiceInvoker = mock(EventServiceInvoker.class);
    eventConsumer = new EventConsumer(eventServiceInvoker);
  }

  @Test
  void file_upload_event_is_ack_if_eventServiceInvoker_succeeded() {
    TypedFileSaved fileUploaded = new TypedFileSaved(FileSaved.builder()
        .fileId(null)
        .fileType(FileType.INVOICE)
        .fileAsBytes(null)
        .accountId(JOE_DOE_ACCOUNT_ID)
        .build());

    Runnable acknowledger = mock(Runnable.class);

    eventConsumer.accept(
        List.of(new EventConsumer.AcknowledgeableTypedEvent(fileUploaded, acknowledger)));

    verify(eventServiceInvoker, timeout(TIMEOUT.toMillis())).accept(fileUploaded);
    verify(acknowledger, timeout(TIMEOUT.toMillis())).run();
  }

  @Test
  void file_upload_event_is_not_ack_if_eventServiceInvoker_failed() {
    TypedFileSaved fileUploaded = new TypedFileSaved(FileSaved.builder()
        .fileId(null)
        .fileType(FileType.INVOICE)
        .fileAsBytes(null)
        .accountId(JOE_DOE_ACCOUNT_ID)
        .build());
    Runnable acknowledger = mock(Runnable.class);
    doThrow(RuntimeException.class).when(eventServiceInvoker).accept(fileUploaded);

    eventConsumer.accept(
        List.of(new EventConsumer.AcknowledgeableTypedEvent(fileUploaded, acknowledger)));

    verify(eventServiceInvoker, timeout(TIMEOUT.toMillis())).accept(fileUploaded);
    verify(acknowledger, timeout(TIMEOUT.toMillis()).times(0)).run();
  }


  @Test
  void email_sent_event_is_ack_if_eventServiceInvoker_succeeded() {
    TypedMailSent emailSent = new TypedMailSent(MailSent.builder()
        .subject(null)
        .recipient(null)
        .attachmentAsBytes(null)
        .htmlBody(null)
        .attachmentName(null)
        .build());
    Runnable acknowledger = mock(Runnable.class);

    eventConsumer.accept(
        List.of(new EventConsumer.AcknowledgeableTypedEvent(emailSent, acknowledger)));

    verify(eventServiceInvoker, timeout(TIMEOUT.toMillis())).accept(emailSent);
    verify(acknowledger, timeout(TIMEOUT.toMillis())).run();
  }

  @Test
  void email_sent_event_is_not_ack_if_eventServiceInvoker_failed() {
    TypedMailSent emailSent = new TypedMailSent(MailSent.builder()
        .subject(null)
        .recipient(null)
        .attachmentAsBytes(null)
        .htmlBody(null)
        .attachmentName(null)
        .build());
    Runnable acknowledger = mock(Runnable.class);
    doThrow(RuntimeException.class).when(eventServiceInvoker).accept(emailSent);

    eventConsumer.accept(
        List.of(new EventConsumer.AcknowledgeableTypedEvent(emailSent, acknowledger)));

    verify(eventServiceInvoker, timeout(TIMEOUT.toMillis())).accept(emailSent);
    verify(acknowledger, timeout(TIMEOUT.toMillis()).times(0)).run();
  }
}