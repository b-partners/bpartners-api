package app.bpartners.api.repository.swan.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenResponse {
  @JsonProperty
  private String access_token;
  @JsonProperty
  private String id_token;
  @JsonProperty
  private String expires_in;
  @JsonProperty
  private String refresh_token;
  @JsonProperty
  private String scope;
  @JsonProperty
  private String token_type;
}


