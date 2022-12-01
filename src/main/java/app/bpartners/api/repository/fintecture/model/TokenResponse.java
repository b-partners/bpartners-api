package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenResponse {
  private static final String JSON_PROPERTY_TOKEN_TYPE = "token_type";
  private static final String JSON_PROPERTY_ACCESS_TOKEN = "access_token";
  private static final String JSON_PROPERTY_EXPIRES_IN = "expires_in";

  private String tokenType;
  private String accessToken;
  private String expiresIn;

  @JsonProperty(JSON_PROPERTY_TOKEN_TYPE)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getTokenType() {
    return tokenType;
  }

  @JsonProperty(JSON_PROPERTY_ACCESS_TOKEN)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getAccessToken() {
    return accessToken;
  }

  @JsonProperty(JSON_PROPERTY_EXPIRES_IN)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getExpiresIn() {
    return expiresIn;
  }

}
