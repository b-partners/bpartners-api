package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import java.io.Serializable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TypedInvoiceRelaunchSaved implements TypedEvent {

  private final InvoiceRelaunchSaved invoiceRelaunchSaved;

  @Override
  public String getTypeName() {
    return InvoiceRelaunchSaved.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return invoiceRelaunchSaved;
  }
}