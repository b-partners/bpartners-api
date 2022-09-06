package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.model.Transaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class TransactionResponse {
  private static final String JSON_PROPERTY_DATA = "data";
  private Data data;

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  public static class Data {
    private static final String JSON_PROPERTY_ACCOUNTS = "accounts";
    private Accounts accounts;

    @JsonProperty(JSON_PROPERTY_ACCOUNTS)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Accounts getAccounts() {
      return accounts;
    }
  }

  public static class Accounts {
    private static final String JSON_PROPERTY_EDGES = "edges";
    private List<Edge> edges;

    @JsonProperty(JSON_PROPERTY_EDGES)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public List<Edge> getEdges() {
      return edges;
    }
  }

  public static class Edge {
    private static final String JSON_PROPERTY_NODE = "node";
    private Node node;

    @JsonProperty(JSON_PROPERTY_NODE)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Node getNode() {
      return node;
    }
  }

  public static class Node {
    private static final String JSON_PROPERTY_TRANSACTION = "transactions";
    private Transactions transactions;

    @JsonProperty(JSON_PROPERTY_TRANSACTION)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Transactions getTransactions() {
      return transactions;
    }
  }

  public static class Transactions {
    private static final String JSON_PROPERTY_SWAN_TRANSACTION = "edges";
    private List<Transaction> edges;

    @JsonProperty(JSON_PROPERTY_SWAN_TRANSACTION)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public List<Transaction> getEdges() {
      return edges;
    }
  }
}
