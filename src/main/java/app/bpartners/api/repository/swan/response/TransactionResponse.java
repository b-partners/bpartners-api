package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.model.Transaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
  private static final String JSON_PROPERTY_DATA = "data";
  private Data data;

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Data {
    private static final String JSON_PROPERTY_ACCOUNT = "account";
    private Account account;

    @JsonProperty(JSON_PROPERTY_ACCOUNT)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Account getAccount() {
      return account;
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Account {
    private static final String JSON_PROPERTY_TRANSACTIONS = "transactions";
    private Transactions transactions;

    @JsonProperty(JSON_PROPERTY_TRANSACTIONS)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Transactions getTransactions() {
      return transactions;
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
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
