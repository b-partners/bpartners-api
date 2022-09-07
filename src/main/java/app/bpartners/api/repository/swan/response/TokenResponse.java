package app.bpartners.api.repository.swan.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {
  public static final String JSON_PROPERTY_IDTOKEN = "id_token";
  public static final String JSON_PROPERTY_EXPIRESIN = "expires_in";
  public static final String JSON_PROPERTY_REFRESHTOKEN = "refresh_token";
  public static final String JSON_PROPERTY_SCOPE = "scope";
  public static final String JSON_PROPERTY_TOKENTYPE = "token_type";
  public static final String JSON_PROPERTY_ACCESSTOKEN = "access_token";
  private String idToken;
  private String expiresIn;
  private String refreshToken;
  private String scope;
  private String tokenType;
  private String accessToken;

  @JsonProperty(JSON_PROPERTY_IDTOKEN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getIdToken() {
    return idToken;
  }

  @JsonProperty(JSON_PROPERTY_REFRESHTOKEN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getRefreshToken() {
    return refreshToken;
  }

  @JsonProperty(JSON_PROPERTY_SCOPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getScope() {
    return scope;
  }

  @JsonProperty(JSON_PROPERTY_TOKENTYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getTokenType() {
    return tokenType;
  }

  @JsonProperty(JSON_PROPERTY_ACCESSTOKEN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getAccessToken() {
    return accessToken;
  }

  @JsonProperty(JSON_PROPERTY_EXPIRESIN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getExpiresIn() {
    return expiresIn;
  }
}


