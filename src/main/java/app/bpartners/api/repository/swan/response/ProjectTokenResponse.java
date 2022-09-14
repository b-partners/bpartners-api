package app.bpartners.api.repository.swan.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProjectTokenResponse {
  public static final String JSON_PROPERTY_EXPIRES_IN = "expires_in";
  public static final String JSON_PROPERTY_SCOPE = "scope";
  public static final String JSON_PROPERTY_TOKEN_TYPE = "token_type";
  public static final String JSON_PROPERTY_ACCESS_TOKEN = "access_token";
  private String expiresIn;
  private String scope;
  private String tokenType;
  private String accessToken;

  @JsonProperty(JSON_PROPERTY_SCOPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getScope() {
    return scope;
  }

  @JsonProperty(JSON_PROPERTY_TOKEN_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getTokenType() {
    return tokenType;
  }

  @JsonProperty(JSON_PROPERTY_ACCESS_TOKEN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getAccessToken() {
    return accessToken;
  }

  @JsonProperty(JSON_PROPERTY_EXPIRES_IN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getExpiresIn() {
    return expiresIn;
  }
}

