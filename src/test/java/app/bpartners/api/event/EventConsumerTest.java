package app.bpartners.api.event;

import static org.mockito.Mockito.*;

import app.bpartners.api.concurrency.Workers;
import app.bpartners.api.endpoint.event.EventConsumer;
import app.bpartners.api.endpoint.event.EventServiceInvoker;
import app.bpartners.api.endpoint.event.gen.InvoiceRelaunchSaved;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventConsumerTest {
  EventConsumer eventConsumer;
  EventServiceInvoker eventServiceInvoker;
  Workers<Void> workers;

  static final Duration TIMEOUT = Duration.ofSeconds(3);

  @BeforeEach
  void setUp() {
    eventServiceInvoker = mock(EventServiceInvoker.class);
    workers = new Workers<>();
    eventConsumer = new EventConsumer(workers, eventServiceInvoker);
  }

  @Test
  void email_sent_event_is_ack_if_eventServiceInvoker_succeeded() {
    InvoiceRelaunchSaved emailSent =
        InvoiceRelaunchSaved.builder()
            .subject(null)
            .recipient(null)
            .htmlBody(null)
            .attachmentName(null)
            .invoice(null)
            .accountHolder(null)
            .logoFileId(null)
            .build();
    Runnable acknowledger = mock(Runnable.class);

    eventConsumer.accept(
        List.of(
            new EventConsumer.AcknowledgeableTypedEvent(
                new EventConsumer.TypedEvent(
                    "app.bpartners.api.endpoint.event.gen.InvoiceRelaunchSaved", emailSent),
                acknowledger)));

    verify(eventServiceInvoker, timeout(TIMEOUT.toMillis()))
        .accept(
            new EventConsumer.TypedEvent(
                "app.bpartners.api.endpoint.event.gen.InvoiceRelaunchSaved", emailSent));
    verify(acknowledger, timeout(TIMEOUT.toMillis())).run();
  }

  @Test
  void email_sent_event_is_not_ack_if_eventServiceInvoker_failed() {
    EventConsumer.AcknowledgeableTypedEvent acknowledgeableTypedEventMock =
        mock(EventConsumer.AcknowledgeableTypedEvent.class);

    InvoiceRelaunchSaved emailSent =
        InvoiceRelaunchSaved.builder()
            .subject(null)
            .recipient(null)
            .htmlBody(null)
            .attachmentName(null)
            .invoice(null)
            .accountHolder(null)
            .logoFileId(null)
            .build();
    Runnable acknowledger = mock(Runnable.class);
    doThrow(RuntimeException.class)
        .when(eventServiceInvoker)
        .accept(
            new EventConsumer.TypedEvent(
                "app.bpartners.api.endpoint.event.gen.InvoiceRelaunchSaved", emailSent));

    eventConsumer.accept(
        List.of(
            new EventConsumer.AcknowledgeableTypedEvent(
                new EventConsumer.TypedEvent(
                    "app.bpartners.api.endpoint.event.gen.InvoiceRelaunchSaved", emailSent),
                acknowledger)));

    verify(acknowledgeableTypedEventMock, never()).ack();
    verify(eventServiceInvoker, timeout(TIMEOUT.toMillis()))
        .accept(
            new EventConsumer.TypedEvent(
                "app.bpartners.api.endpoint.event.gen.InvoiceRelaunchSaved", emailSent));
    verify(acknowledger, timeout(TIMEOUT.toMillis()).times(0)).run();
  }
}
