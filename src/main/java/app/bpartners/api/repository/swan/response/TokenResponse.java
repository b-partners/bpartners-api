package app.bpartners.api.repository.swan.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse extends ProjectTokenResponse {
  private static final String JSON_PROPERTY_ID_TOKEN = "id_token";
  private static final String JSON_PROPERTY_REFRESH_TOKEN = "refresh_token";
  private String idToken;
  private String  refreshToken;

  public TokenResponse(String expiresIn, String scope, String tokenType, String accessToken) {
    super(expiresIn, scope, tokenType, accessToken);
  }

  @JsonProperty(JSON_PROPERTY_ID_TOKEN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getIdToken() {
    return idToken;
  }

  @JsonProperty(JSON_PROPERTY_REFRESH_TOKEN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getRefreshToken() {
    return refreshToken;
  }
}


