package app.bpartners.api.repository.swan.response;


import app.bpartners.api.repository.swan.schema.SwanAccount;
import java.util.List;

public class AccountResponse {
  public Data data;

  public static class Data {
    public Accounts accounts;
  }

  public static class Accounts {
    public List<Edge> edges;
  }

  public static class Edge {
    public SwanAccount node;
  }
}
