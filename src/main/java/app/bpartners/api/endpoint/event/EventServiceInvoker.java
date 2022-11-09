package app.bpartners.api.endpoint.event;

import app.bpartners.api.endpoint.event.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.endpoint.event.model.gen.MailSent;
import app.bpartners.api.service.FileSavedService;
import app.bpartners.api.service.InvoiceCrupdatedService;
import app.bpartners.api.service.MailSentService;
import java.io.Serializable;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class EventServiceInvoker implements Consumer<TypedEvent> {
  private final MailSentService mailSentService;
  private final FileSavedService fileSavedService;
  private final InvoiceCrupdatedService invoiceCrupdatedService;

  @Override
  public void accept(TypedEvent typedEvent) {
    Serializable payload = typedEvent.getPayload();
    if (MailSent.class.getTypeName().equals(typedEvent.getTypeName())) {
      mailSentService.accept((MailSent) payload);
    } else if (FileSaved.class.getTypeName().equals(typedEvent.getTypeName())) {
      fileSavedService.accept((FileSaved) payload);
    } else if (InvoiceCrupdated.class.getTypeName().equals(typedEvent.getTypeName())) {
      invoiceCrupdatedService.accept((InvoiceCrupdated) payload);
    } else {
      log.error("Unexpected type for event={}", typedEvent);
    }
  }
}
