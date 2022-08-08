package app.bpartners.api.endpoint.rest.security.swan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwanConf {
  private static String clientId;
  private static String clientSecret;
  private static String redirectUri;
  private static final String oauth2Provider = "https://oauth.swan.io/oauth2";

  public SwanConf(
      @Value("${swan.api.clientId") String clientId,
      @Value("${swan.api.clientSecret") String clientSecret,
      @Value("${swan.api.redirectUri") String redirectUri
  ) {
    SwanConf.clientId = clientId;
    SwanConf.clientSecret = clientSecret;
    SwanConf.redirectUri = redirectUri;
  }

  public static String getClientId() {
    return clientId;
  }

  public static String getClientSecret() {
    return clientSecret;
  }

  public static String getRedirectUri() {
    return redirectUri;
  }

  public static String getAuthUrl() {
    return
        String.format(
            "%s/auth?response_type=code&client_id=%s&redirect_uri=%s"
                + "&scope=openid%20offline%20idverified&state=", oauth2Provider, clientId,
            redirectUri);
  }

  public static String getTokenProviderUrl() {
    return String.format("%s/token", oauth2Provider);
  }
}
