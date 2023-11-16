package app.bpartners.api.model.prospect;

import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ProspectStatusHistory {
  private ProspectStatus status;
  private Instant updatedAt;

  public static List<ProspectStatusHistory> defaultStatusHistory() {
    return List.of(ProspectStatusHistory.builder()
        .status(TO_CONTACT)
        .updatedAt(Instant.now())
        .build());
  }
}
