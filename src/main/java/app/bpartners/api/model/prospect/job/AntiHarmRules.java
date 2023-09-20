package app.bpartners.api.model.prospect.job;

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
}
