package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.ProspectEvaluationJobInitiated;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
//TODO: add docs in EventBridge
public class TypedProspectEvaluationJobInitiated implements TypedEvent {

  private final ProspectEvaluationJobInitiated evaluationJobInitiated;

  @Override
  public String getTypeName() {
    return ProspectEvaluationJobInitiated.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return evaluationJobInitiated;
  }
}
