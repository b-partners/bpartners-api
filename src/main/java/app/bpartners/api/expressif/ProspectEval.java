package app.bpartners.api.expressif;

import app.bpartners.api.model.Prospect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class ProspectEval<T> {
  private NewProspect newProspect;

  private Boolean lockSmith;
  private Boolean antiHarm;
  private Boolean insectControl;
  private Boolean disinfection;
  private Boolean ratRemoval;
  private Boolean professionalCustomer;
  private Boolean particularCustomer;

  private T depaRule;
}
