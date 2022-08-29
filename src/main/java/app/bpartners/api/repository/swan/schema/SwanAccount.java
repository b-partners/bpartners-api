package app.bpartners.api.repository.swan.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SwanAccount {
  @JsonProperty
  private String id;
  @JsonProperty
  private String name;
  @JsonProperty
  private String IBAN;
  @JsonProperty
  private String BIC;
}
