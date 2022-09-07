package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.model.AccountHolder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AccountHolderResponse {
  private static final String JSON_PROPERTY_DATA = "data";
  private Data data;

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  public static class Data {
    private static final String JSON_PROPERTY_ACCOUNTHOLDERS = "accountHolders";
    private AccountHolders accountHolders;

    @JsonProperty(JSON_PROPERTY_ACCOUNTHOLDERS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public AccountHolders getAccountHolders() {
      return accountHolders;
    }
  }

  public static class AccountHolders {
    private static final String JSON_PROPERTY_EDGES = "edges";
    private List<Edge> edges;

    @JsonProperty(JSON_PROPERTY_EDGES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<Edge> getEdges() {
      return edges;
    }
  }

  public static class Edge {
    private static final String JSON_PROPERTY_ACCOUNTHOLDER = "node";
    private AccountHolder node;

    @JsonProperty(JSON_PROPERTY_ACCOUNTHOLDER)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public AccountHolder getNode() {
      return node;
    }
  }
}