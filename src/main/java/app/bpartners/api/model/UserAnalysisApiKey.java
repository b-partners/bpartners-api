package app.bpartners.api.model;

import java.time.Instant;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class UserAnalysisApiKey {
  private String id;
  private String userId;
  private Instant creationDatetime;
  private Instant expirationDatetime;
  private String apiKey;
}
