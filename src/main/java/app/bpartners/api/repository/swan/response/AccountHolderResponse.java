package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHolderResponse {
  private static final String JSON_PROPERTY_DATA = "data";
  private Data data;

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Data {
    private static final String JSON_PROPERTY_ACCOUNT_HOLDERS = "accountHolders";
    private AccountHolders accountHolders;

    @JsonProperty(JSON_PROPERTY_ACCOUNT_HOLDERS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public AccountHolders getAccountHolders() {
      return accountHolders;
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AccountHolders {
    private static final String JSON_PROPERTY_EDGES = "edges";
    private List<Edge> edges;

    @JsonProperty(JSON_PROPERTY_EDGES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<Edge> getEdges() {
      return edges;
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  @Builder
  public static class Edge {
    private static final String JSON_PROPERTY_ACCOUNTHOLDER = "node";
    private SwanAccountHolder node;

    @JsonProperty(JSON_PROPERTY_ACCOUNTHOLDER)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public SwanAccountHolder getNode() {
      return node;
    }
  }
}