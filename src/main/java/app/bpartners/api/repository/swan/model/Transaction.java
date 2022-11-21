package app.bpartners.api.repository.swan.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class Transaction {
  private Node node;

  @JsonProperty("node")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Node getNode() {
    return node;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Amount {
    private String currency;
    private Double value;

    @JsonProperty("currency")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCurrency() {
      return currency;
    }

    @JsonProperty("value")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Double getValue() {
      return value;
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Node {
    private String id;
    private String label;
    private String reference;
    private Amount amount;
    private Instant createdAt;
    private String side;

    @JsonProperty("id")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getId() {
      return id;
    }

    @JsonProperty("label")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getLabel() {
      return label;
    }

    @JsonProperty("reference")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getReference() {
      return reference;
    }

    @JsonProperty("amount")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Amount getAmount() {
      return amount;
    }

    @JsonProperty("createdAt")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Instant getCreatedAt() {
      return createdAt;
    }

    @JsonProperty("side")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getSide() {
      return side;
    }
  }
}
