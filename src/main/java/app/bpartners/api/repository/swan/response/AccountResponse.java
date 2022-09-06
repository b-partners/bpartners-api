package app.bpartners.api.repository.swan.response;


import app.bpartners.api.repository.swan.model.SwanAccount;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AccountResponse {
  private Data data;
  private static final String JSON_PROPERTY_DATA = "data";

  public static class Data {
    private Accounts accounts;
    private static final String JSON_PROPERTY_ACCOUNTS = "accounts";

    @JsonProperty(JSON_PROPERTY_ACCOUNTS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Accounts getAccounts() {
      return accounts;
    }
  }

  public static class Accounts {
    private List<Edge> edges;
    private static final String JSON_PROPERTY_EDGES = "edges";

    @JsonProperty(JSON_PROPERTY_EDGES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<Edge> getEdges() {
      return edges;
    }
  }

  public static class Edge {
    private SwanAccount swanAccount;
    private static final String JSON_PROPERTY_SWAN_ACCOUNT = "node";

    @JsonProperty(JSON_PROPERTY_SWAN_ACCOUNT)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public SwanAccount getSwanAccount() {
      return swanAccount;
    }
  }

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }
}
