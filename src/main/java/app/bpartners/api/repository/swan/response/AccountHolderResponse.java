package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.schema.AccountHolder;
import java.util.List;

public class AccountHolderResponse {
  public Data data;

  public static class Data {
    public AccountHolders accountHolders;
  }

  public static class AccountHolders {
    public List<Edge> edges;
  }

  public static class Edge {
    public AccountHolder node;
  }
}
