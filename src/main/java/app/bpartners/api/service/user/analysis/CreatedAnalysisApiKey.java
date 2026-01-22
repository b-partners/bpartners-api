package app.bpartners.api.service.user.analysis;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatedAnalysisApiKey {
  private String key;
  private Instant creationDatetime;
}
