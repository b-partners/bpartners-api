package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentRedirection {
  private Meta meta;

  @JsonProperty("meta")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Meta getMeta() {
    return meta;
  }

  public static class Meta {
    private String sessionId;
    private String url;

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
