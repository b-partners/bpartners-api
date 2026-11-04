package app.bpartners.api.endpoint.rest.security.dashboard;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class DashboardTokenConf {
  private final byte[] signingKey;
  private final Duration ttl;

  public DashboardTokenConf(
      @Value("${dashboard.token.signing.key}") String signingKey,
      @Value("${dashboard.token.ttl.minutes:15}") long ttlMinutes) {
    this.signingKey = signingKey.getBytes(StandardCharsets.UTF_8);
    this.ttl = Duration.ofMinutes(ttlMinutes);
  }
}
