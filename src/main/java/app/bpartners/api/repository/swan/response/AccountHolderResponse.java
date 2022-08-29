package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.schema.AccountHolder;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@AllArgsConstructor
@Jacksonized
public class AccountHolderResponse {
  @JsonProperty
  private Data data;

  @Getter
  @Setter
  public static class Data {
    @JsonProperty
    private AccountHolders accountHolders;
  }

  @Getter
  @Setter
  public static class AccountHolders {
    @JsonProperty
    private List<Edge> edges;
  }

  @Getter
  @Setter
  public static class Edge {
    @JsonProperty
    private AccountHolder node;
  }
}
