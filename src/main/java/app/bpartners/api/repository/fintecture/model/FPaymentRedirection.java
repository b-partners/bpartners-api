package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FPaymentRedirection {
  @JsonProperty("meta")
  private Meta meta;

  @Builder
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Meta {
    @JsonProperty("status")
    private Integer status;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("url")
    private String url;
  }
}
