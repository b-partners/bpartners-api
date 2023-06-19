package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.CustomerCrupdated;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class TypedCustomerCrupdated implements TypedEvent {
  private final CustomerCrupdated customerCrupdated;

  @Override
  public String getTypeName() {
    return CustomerCrupdated.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return customerCrupdated;
  }
}
