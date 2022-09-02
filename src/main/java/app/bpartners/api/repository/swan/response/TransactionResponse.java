package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.model.SwanTransaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class TransactionResponse {
  private Data data;
  private static final String JSON_PROPERTY_DATA = "data";

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  public static class Data {
    private Accounts accounts;
    private static final String JSON_PROPERTY_ACCOUNTS = "accounts";

    @JsonProperty(JSON_PROPERTY_ACCOUNTS)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Accounts getAccounts() {
      return accounts;
    }
  }

  public static class Accounts {
    private List<Edge> edges;
    private static final String JSON_PROPERTY_EDGES = "edges";

    @JsonProperty(JSON_PROPERTY_EDGES)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public List<Edge> getEdges() {
      return edges;
    }
  }

  public static class Edge {
    private Node node;
    private static final String JSON_PROPERTY_NODE = "node";

    @JsonProperty(JSON_PROPERTY_NODE)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Node getNode() {
      return node;
    }
  }

  public static class Node {
    private Transactions transactions;
    private static final String JSON_PROPERTY_TRANSACTION = "transactions";

    @JsonProperty(JSON_PROPERTY_TRANSACTION)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Transactions getTransactions() {
      return transactions;
    }
  }

  public static class Transactions {
    private List<SwanTransaction> edges;
    private static final String JSON_PROPERTY_SWAN_TRANSACTION = "edges";

    @JsonProperty(JSON_PROPERTY_SWAN_TRANSACTION)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public List<SwanTransaction> getEdges() {
      return edges;
    }
  }
}
