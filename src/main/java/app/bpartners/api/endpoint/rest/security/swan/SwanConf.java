package app.bpartners.api.endpoint.rest.security.swan;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SwanConf {
  public static final String API_DOMAIN_URL = "https://api.swan.io/";
  public static final String BEARER_PREFIX = "Bearer ";
  public static final String SWAN_TOKEN_URL = "https://oauth.swan.io/oauth2/token";
  private String clientId;
  private String clientSecret;
  private String oauthUrl;
  private String swanEnv;


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

  public Map<String, String> getParams() {
    Map<String, String> params = new HashMap<>();
    params.put("client_id", clientId);
    params.put("client_secret", clientSecret);
    params.put("grant_type", "client_credentials");
    return params;
  }
}
