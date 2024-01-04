package app.bpartners.api.model.prospect.job;

import static app.bpartners.api.endpoint.rest.model.InterventionType.DISINFECTION;
import static app.bpartners.api.endpoint.rest.model.InterventionType.INSECT_CONTROL;
import static app.bpartners.api.endpoint.rest.model.InterventionType.RAT_REMOVAL;

import app.bpartners.api.endpoint.rest.model.InterventionType;
import java.util.List;
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
public class AntiHarmRules {
  private String infestationType;
  private List<InterventionType> interventionTypes;

  public boolean isInsectControl() {
    return interventionTypes.contains(INSECT_CONTROL);
  }

  public boolean isDisinfection() {
    return interventionTypes.contains(DISINFECTION);
  }

  public boolean isRatRemoval() {
    return interventionTypes.contains(RAT_REMOVAL);
  }
}
