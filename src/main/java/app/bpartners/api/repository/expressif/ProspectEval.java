package app.bpartners.api.repository.expressif;

import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.expressif.fact.Robbery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderClassName = "Builder", toBuilder = true)
@EqualsAndHashCode
@ToString
public class ProspectEval<T> {
  private String id;
  private String prospectOwnerId;
  private ProspectEvalInfo prospectEvalInfo;
  private Boolean lockSmith;
  private Boolean antiHarm;
  private Boolean insectControl;
  private Boolean disinfection;
  private Boolean ratRemoval;
  private Boolean professionalCustomer;
  private Boolean particularCustomer;

  private T depaRule;

  public boolean isNewIntervention() {
    return depaRule.getClass().getTypeName().equals(NewIntervention.class.getTypeName());
  }

  public boolean isRobbery() {
    return depaRule.getClass().getTypeName().equals(Robbery.class.getTypeName());
  }
}
