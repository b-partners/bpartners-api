package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
public class TokenInitiation {
  private static final String JSON_PROPERTY_GRANT_TYPE = "grantType";
  private static final String JSON_PROPERTY_APP_ID = "appId";
  private static final String JSON_PROPERTY_SCOPE = "scope";
  private String grantType;
  @Value("${fintecture.app.id}")
  private String appId;

  private String scope;

  @JsonProperty(JSON_PROPERTY_GRANT_TYPE)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getGrantType() {
    return "client_credentials";
  }

  @JsonProperty(JSON_PROPERTY_APP_ID)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getAppId() {
    return appId;
  }

  @JsonProperty(JSON_PROPERTY_SCOPE)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getScope() {
    return "PIS";
  }
}
