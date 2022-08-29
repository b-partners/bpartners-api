package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.schema.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionResponse {
  @JsonProperty
  private Data data;

  @Getter
  @Setter

  public static class Data {
    @JsonProperty
    private Account accounts;
  }

  @Getter
  @Setter
  public static class Account {
    @JsonProperty
    private List<Edge> edges;
  }

  @Getter
  @Setter
  public static class Edge {
    @JsonProperty
    private Node node;
  }

  @Getter
  @Setter
  public static class Node {
    @JsonProperty
    private Transactions transactions;
  }

  @Getter
  @Setter
  public static class Transactions {
    @JsonProperty
    private List<Transaction> edges;
  }
}
