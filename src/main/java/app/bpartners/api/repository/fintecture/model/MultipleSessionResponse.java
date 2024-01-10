package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultipleSessionResponse {
  @JsonProperty("data")
  private List<Session> data;

  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Session {
    @JsonProperty("meta")
    private Meta meta;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Meta {
    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("status")
    private String status;
  }
}
