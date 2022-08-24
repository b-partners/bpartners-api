package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.schema.SwanUser;

public class UserResponse {
  public Data data;

  public static class Data {
    public SwanUser user;
}