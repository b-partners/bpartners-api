package app.bpartners.api.model.prospect;

import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import java.time.Instant;
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
public class ProspectStatusHistory {
  private ProspectStatus status;
  private Instant updatedAt;
}
