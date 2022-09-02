package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

@Setter
public class PaymentRedirection {
  private Meta meta;
  private static final String JSON_PROPERTY_META = "meta";

  @JsonProperty(JSON_PROPERTY_META)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Meta getMeta() {
    return meta;
  }

  @Setter
  public static class Meta {
    private String sessionId;
    private static final String JSON_PROPERTY_SESSION = "session_id";

    @JsonProperty(JSON_PROPERTY_SESSION)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getSessionId() {
      return sessionId;
    }


    private String url;
    private static final String JSON_PROPERTY_URL = "url";

    @JsonProperty(JSON_PROPERTY_URL)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getUrl() {
      return url;
    }
  }
}
