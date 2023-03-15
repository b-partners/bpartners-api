package app.bpartners.api.repository.bridge.response;

import app.bpartners.api.repository.bridge.model.BridgeUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@Data
@ToString
public class BridgeUserToken {
  @JsonProperty("user")
  private BridgeUser user;
  @JsonProperty("access_token")
  private String accessToken;
  @JsonProperty("expires_at")
  private Instant expirationDate;

}
