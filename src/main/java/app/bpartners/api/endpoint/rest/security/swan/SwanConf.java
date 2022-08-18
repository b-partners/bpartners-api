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
  private String oauthUrl;
  private String onboardingUrl;
  private String swanEnv;

  public static final String API_DOMAIN_URL = "https://api.swan.io/";

  public static final String BEARER_PREFIX = "Bearer ";

  public SwanConf(
      @Value("${swan.client.id}")
      String clientId,
      @Value("${swan.client.secret}")
      String clientSecret,
      @Value("${swan.redirect.uri}")
      String redirectUri,
      @Value("${swan.oauth.url}")
      String oauthUrl,
      @Value("${swan.env}")
      String swanEnv,
      @Value("https://api.banking.sandbox.swan.io/projects/df47a093-efda-4802-b7ff-8d4946545a5e/onboarding/company/upfront")
      String publicOnboardingUrl
  ) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
    this.oauthUrl = oauthUrl;
    this.swanEnv = swanEnv;
    this.onboardingUrl = publicOnboardingUrl;
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
