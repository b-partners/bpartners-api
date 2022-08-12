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

  public static final String individualOnboardingUrl =
      "https://api.banking.sandbox.swan.io/projects/df47a093-efda-4802-b7ff-8d4946545a5e/onboarding/individual/upfront";

  public static final String companyOnboardingUrl =
      "https://api.banking.sandbox.swan.io/projects/df47a093-efda-4802-b7ff-8d4946545a5e/onboarding/company/upfront";

  public SwanConf(
      @Value("${swan.client.id}")
      String clientId,
      @Value("${swan.client.secret}")
      String clientSecret,
      @Value("${swan.redirect.uri}")
      String redirectUri) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
  }
}
