package app.bpartners.api.repository.swan.response;

import app.bpartners.api.repository.swan.model.SwanUser;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
  private Data data;
  private static final String JSON_PROPERTY_DATA = "data";

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Data {
    private SwanUser user;
    private static final String JSON_PROPERTY_SWAN_USER = "user";

    @JsonProperty(JSON_PROPERTY_SWAN_USER)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public SwanUser getUser() {
      return user;
    }
  }

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }
}


