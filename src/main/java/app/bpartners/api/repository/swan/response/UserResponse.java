package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.schema.SwanUser;
import lombok.Getter;

@Getter
public class UserResponse {
  private Data data;

  public static class Data {
    private SwanUser user;
  }
}