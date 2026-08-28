package app.bpartners.api.model.prospect;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class Analyse {
  private String id;
  private Prospect prospect;
  private Map<String, String> metadata;
  private Instant createdAt;
  private Instant updatedAt;
}
