package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.model.SwanUser;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserResponse {
  private Data data;
  private static final String JSON_PROPERTY_DATA = "data";

  public static class Data {
    private SwanUser swanUser;
    private static final String JSON_PROPERTY_SWAN_USER = "user";

    @JsonProperty(JSON_PROPERTY_SWAN_USER)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public SwanUser getSwanUser() {
      return swanUser;
    }
  }

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }
}


