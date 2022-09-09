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
public class TokenResponse {
  private static final String JSON_PROPERTY_TOKEN_TYPE = "token_type";
  private static final String JSON_PROPERTY_ACCESS_TOKEN = "access_token";
  private static final String JSON_PROPERTY_EXPIRE_IN = "expires_in";
  private String token_type;
  private String access_token;
  private String expires_in;

  @JsonProperty(JSON_PROPERTY_TOKEN_TYPE)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getToken_type() {
    return token_type;
  }

  @JsonProperty(JSON_PROPERTY_ACCESS_TOKEN)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getAccess_token() {
    return access_token;
  }

  @JsonProperty(JSON_PROPERTY_EXPIRE_IN)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getExpires_in() {
    return expires_in;
  }
}
