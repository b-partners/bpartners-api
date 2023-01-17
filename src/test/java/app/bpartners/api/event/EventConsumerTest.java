package app.bpartners.api.event;

import app.bpartners.api.endpoint.event.EventConsumer;
import app.bpartners.api.endpoint.event.EventServiceInvoker;
import app.bpartners.api.endpoint.event.model.TypedInvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
  void email_sent_event_is_ack_if_eventServiceInvoker_succeeded() {
    TypedInvoiceRelaunchSaved emailSent =
        new TypedInvoiceRelaunchSaved(InvoiceRelaunchSaved.builder()
            .subject(null)
            .recipient(null)
            .htmlBody(null)
            .attachmentName(null)
            .invoice(null)
            .accountHolder(null)
            .logoFileId(null)
            .build());
    Runnable acknowledger = mock(Runnable.class);

    eventConsumer.accept(
        List.of(new EventConsumer.AcknowledgeableTypedEvent(emailSent, acknowledger)));

    verify(eventServiceInvoker, timeout(TIMEOUT.toMillis())).accept(emailSent);
    verify(acknowledger, timeout(TIMEOUT.toMillis())).run();
  }

  @Test
  void email_sent_event_is_not_ack_if_eventServiceInvoker_failed() {
    TypedInvoiceRelaunchSaved emailSent =
        new TypedInvoiceRelaunchSaved(InvoiceRelaunchSaved.builder()
            .subject(null)
            .recipient(null)
            .htmlBody(null)
            .attachmentName(null)
            .invoice(null)
            .accountHolder(null)
            .logoFileId(null)
            .build());
    Runnable acknowledger = mock(Runnable.class);
    doThrow(RuntimeException.class).when(eventServiceInvoker).accept(emailSent);

    eventConsumer.accept(
        List.of(new EventConsumer.AcknowledgeableTypedEvent(emailSent, acknowledger)));

    verify(eventServiceInvoker, timeout(TIMEOUT.toMillis())).accept(emailSent);
    verify(acknowledger, timeout(TIMEOUT.toMillis()).times(0)).run();
  }
}