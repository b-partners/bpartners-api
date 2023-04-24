package app.bpartners.api.endpoint.event;

import app.bpartners.api.endpoint.event.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.model.gen.UserUpserted;
import app.bpartners.api.service.InvoiceCrupdatedService;
import app.bpartners.api.service.InvoiceRelaunchSavedService;
import app.bpartners.api.service.UserUpsertedService;
import java.io.Serializable;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class EventServiceInvoker implements Consumer<TypedEvent> {
  private final InvoiceRelaunchSavedService invoiceRelaunchSavedService;
  private final InvoiceCrupdatedService invoiceCrupdatedService;
  private final UserUpsertedService userUpsertedService;

  @Override
  public void accept(TypedEvent typedEvent) {
    Serializable payload = typedEvent.getPayload();
    if (InvoiceRelaunchSaved.class.getTypeName().equals(typedEvent.getTypeName())) {
      invoiceRelaunchSavedService.accept((InvoiceRelaunchSaved) payload);
    } else if (InvoiceCrupdated.class.getTypeName().equals(typedEvent.getTypeName())) {
      invoiceCrupdatedService.accept((InvoiceCrupdated) payload);
    } else if (UserUpserted.class.getTypeName().equals(typedEvent.getTypeName())) {
      userUpsertedService.accept((UserUpserted) payload);
    } else {
      log.error("Unexpected type for event={}", typedEvent);
    }
  }
}
