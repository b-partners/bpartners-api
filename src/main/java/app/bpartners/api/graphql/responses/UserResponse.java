package app.bpartners.api.graphql.responses;

import app.bpartners.api.graphql.schemas.SwanUser;

public class UserResponse {
  public Data data;

  public static class Data {
    public SwanUser user;
  }
}


