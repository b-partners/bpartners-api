package app.bpartners.api.model.prospect.job;

import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.endpoint.rest.model.ProfessionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class EvaluationRules {
  private ProfessionType profession;
  private AntiHarmRules antiHarmRules;
  private NewInterventionOption interventionOption;
}
