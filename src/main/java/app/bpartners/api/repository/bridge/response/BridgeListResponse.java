package app.bpartners.api.repository.bridge.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeListResponse<T> {
  @JsonProperty("resources")
  private List<T> resources;

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
