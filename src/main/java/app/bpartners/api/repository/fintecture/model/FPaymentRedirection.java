package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FPaymentRedirection {
  private Meta meta;

  @JsonProperty("meta")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Meta getMeta() {
    return meta;
  }

  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Meta {
    private Integer status;
    private String sessionId;
    private String url;

    @JsonProperty("status")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Integer getStatus() {
      return status;
    }

    @JsonProperty("session_id")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getSessionId() {
      return sessionId;
    }

    @JsonProperty("url")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getUrl() {
      return url;
    }
  }
}
