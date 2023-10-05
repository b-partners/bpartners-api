package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.ProspectUpdated;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
//TODO: add docs in EventBridge
public class TypedProspectUpdated implements TypedEvent {

  private final ProspectUpdated prospectUpdated;

  @Override
  public String getTypeName() {
    return ProspectUpdated.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return prospectUpdated;
  }
}
