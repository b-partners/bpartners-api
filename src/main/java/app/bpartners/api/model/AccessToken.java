package app.bpartners.api.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class AccessToken {
  private String id;
  private String value;
  private String refreshToken;
  private Long expirationTimeInSeconds;
  private Instant creationDatetime;
  private Instant expirationDatetime;
}
