package app.bpartners.api.endpoint.rest.security.swan;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SwanConf {
  private String clientId;
  private String clientSecret;
  private String oauthUrl;
  private String swanEnv;

  public static final String API_DOMAIN_URL = "https://api.swan.io/";

  public static final String BEARER_PREFIX = "Bearer ";

  public SwanConf(
      @Value("${swan.client.id}")
      String clientId,
      @Value("${swan.client.secret}")
      String clientSecret,
      @Value("${swan.oauth.url}")
      String oauthUrl,
      @Value("${swan.env}")
      String swanEnv
  ) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.oauthUrl = oauthUrl;
    this.swanEnv = swanEnv;
  }

  public String getApiUrl() {
    return API_DOMAIN_URL + swanEnv + "/graphql";
  }

  public String getTokenProviderUrl() {
    return oauthUrl + "/token";
  }

  public String getAuthProviderUrl() {
    return oauthUrl + "/auth";
  }
}
