package app.bpartners.api.endpoint.rest.security.swan;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SwanConf {
  private String clientId;
  private String clientSecret;
  private String redirectUri;

  public SwanConf(
      @Value("${api.swan.clientId}")
      String clientId,
      @Value("${api.swan.clientSecret}")
      String clientSecret,
      @Value("${api.swan.redirect.uri}")
      String redirectUri) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
  }
}
