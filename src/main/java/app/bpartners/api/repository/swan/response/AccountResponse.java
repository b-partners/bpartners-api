package app.bpartners.api.repository.swan.response;


import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.swan.model.SwanAccount;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.jdi.InternalException;
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
      if (edges.size() > 1) {
        throw new NotImplementedException("One user with one account is supported for "
            + "now");
      } else if (edges.size() == 0) {
        throw new InternalException("No account was fetched");
      }
      return edges;
    }
  }

  public static class Edge {
    private SwanAccount node;
    private static final String JSON_PROPERTY_SWAN_ACCOUNT = "node";

    @JsonProperty(JSON_PROPERTY_SWAN_ACCOUNT)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public SwanAccount getNode() {
      return node;
    }
  }

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }
}
