package app.bpartners.api.endpoint.event;

import app.bpartners.api.endpoint.event.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.gen.FileUploaded;
import app.bpartners.api.endpoint.event.model.gen.MailSent;
import app.bpartners.api.service.FileUploadedService;
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
  private final FileUploadedService fileUploadedService;

  @Override
  public void accept(TypedEvent typedEvent) {
    Serializable payload = typedEvent.getPayload();
    if (MailSent.class.getTypeName().equals(typedEvent.getTypeName())) {
      mailSentService.accept((MailSent) payload);
    } else if (FileUploaded.class.getTypeName().equals(typedEvent.getTypeName())) {
      fileUploadedService.accept((FileUploaded) payload);
    } else {
      log.error("Unexpected type for event={}", typedEvent);
    }
  }
}
