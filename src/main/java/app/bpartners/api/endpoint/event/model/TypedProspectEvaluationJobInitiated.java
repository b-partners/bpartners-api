package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
//TODO: add docs in EventBridge
public class TypedProspectEvaluationJobInitiated implements TypedEvent {

  private final ProspectEvaluationJobRunner evaluationJobRunner;

  @Override
  public String getTypeName() {
    return ProspectEvaluationJobRunner.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return evaluationJobRunner;
  }
}
