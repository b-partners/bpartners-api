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
  public String onboardingUrl;

  public SwanConf(
      @Value("${swan.client.id}")
      String clientId,
      @Value("${swan.client.secret}")
      String clientSecret,
      @Value("${swan.redirect.uri}")
      String redirectUri,
      @Value("swan.oauth.url")
      String oauthUrl,
      @Value("https://api.banking.sandbox.swan.io/projects/df47a093-efda-4802-b7ff-8d4946545a5e/onboarding/company/upfront")
      String onboardingUrl
  ) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
    this.oauthUrl = oauthUrl;
    this.onboardingUrl = onboardingUrl;
  }
}
