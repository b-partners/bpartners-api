package app.bpartners.api.repository.swan.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Transaction {
  private Node node;

  @Getter
  @Setter
  public static class Amount {
    @JsonProperty
    private String currency;
    @JsonProperty
    private Double value;
  }

  @Getter
  @Setter
  public static class Node {
    @JsonProperty
    private String id;
    @JsonProperty
    private String label;
    @JsonProperty
    private String reference;
    @JsonProperty
    private Amount amount;
    @JsonProperty
    private Instant createdAt;

  }
}
