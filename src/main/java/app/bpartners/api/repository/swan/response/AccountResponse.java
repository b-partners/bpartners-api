package app.bpartners.api.repository.swan.response;


import app.bpartners.api.repository.swan.schema.SwanAccount;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountResponse {
  @JsonProperty
  private Data data;

  @Getter
  @Setter
  public static class Data {
    @JsonProperty
    private Accounts accounts;
  }

  @Getter
  @Setter
  public static class Accounts {
    @JsonProperty
    private List<Edge> edges;
  }

  @Getter
  @Setter
  public static class Edge {
    @JsonProperty
    private SwanAccount node;
  }
}
