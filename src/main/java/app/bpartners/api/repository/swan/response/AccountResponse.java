package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.schema.SwanAccount;

public class AccountResponse {
  public Data data;

  public static class Data {
    public SwanAccount account;
  }
}


