package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ProspectHistory {
  private String id;
  private String idAccountHolder;
  private String idProspect;
  private ProspectStatus status;
  private Instant updatedAt;
}
