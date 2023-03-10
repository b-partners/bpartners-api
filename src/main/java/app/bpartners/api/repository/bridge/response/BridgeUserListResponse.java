package app.bpartners.api.repository.bridge.response;

import app.bpartners.api.repository.bridge.model.BridgeUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@Data
@ToString
public class BridgeUserListResponse {
  @JsonProperty("resources")
  private List<BridgeUser> users;
  @JsonProperty("pagination")
  private Pagination pagination;

  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Data
  public static class Pagination {
    @JsonProperty("next_uri")
    private String nextUri;
  }
}
