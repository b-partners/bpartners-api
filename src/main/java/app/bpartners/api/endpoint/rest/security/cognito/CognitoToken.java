package app.bpartners.api.endpoint.rest.security.cognito;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class CognitoToken {
  @JsonProperty("id_token")
  private String idToken;
  @JsonProperty("refresh_token")
  private String refreshToken;
  @JsonProperty("expires_in")
  private Integer expiresIn;
}
