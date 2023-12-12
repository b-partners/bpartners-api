package app.bpartners.api.endpoint.event.gen;

import app.bpartners.api.model.prospect.Prospect;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

//TODO: use generated from EventBridge instead
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ProspectUpdated implements Serializable {
  private Prospect prospect;
  private Instant updatedAt;
}
