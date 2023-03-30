package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.model.SwanTransaction;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OneTransactionResponse {
  private Data data;
  private static final String JSON_PROPERTY_DATA = "data";

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Data {
    private SwanTransaction.Node transaction;
    private static final String JSON_PROPERTY_TRANSACTION = "transaction";

    @JsonProperty(JSON_PROPERTY_TRANSACTION)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public SwanTransaction.Node getTransaction() {
      return transaction;
    }
  }
}
