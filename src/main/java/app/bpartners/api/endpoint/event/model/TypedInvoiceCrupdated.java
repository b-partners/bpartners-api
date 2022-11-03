package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import java.io.Serializable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TypedInvoiceCrupdated implements TypedEvent {

  private final InvoiceCrupdated invoiceCrupdated;

  @Override
  public String getTypeName() {
    return InvoiceCrupdated.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return invoiceCrupdated;
  }
}
