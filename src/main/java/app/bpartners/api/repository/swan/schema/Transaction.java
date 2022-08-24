package app.bpartners.api.repository.swan.schema;

import java.time.Instant;

public class Transaction {
  public Node node;

  public static class Amount {
    public String currency;
    public Double value;
  }

  public static class Node {
    public String id;
    public String label;
    public String reference;
    public Amount amount;
    public Instant createdAt;

  }
}
