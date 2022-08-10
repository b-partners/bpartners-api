package app.bpartners.api.endpoint.rest.security.swan;

import app.bpartners.api.endpoint.rest.security.JWTConf;
import app.bpartners.api.model.exception.ApiException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static app.bpartners.api.endpoint.rest.security.JWTConf.getContextConfigurableJWTProcessor;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Configuration
public class SwanConf {
  private final String clientId;
  private final String clientSecret;
  private final String redirectUri;
  private final JWTConf jwtConf;
  private static final String oauthUrl = "https://oauth.swan.io/";

  public SwanConf(
      @Value("${swan.api.clientId") String clientId,
      @Value("${swan.api.clientSecret") String clientSecret,
      @Value("${swan.api.redirectUri") String redirectUri,
      JWTConf jwtConf
  ) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
    this.jwtConf = jwtConf;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public String getAuthUrl() {
    return
        String.format(oauthUrl
                + "oauth2/auth?response_type=code&client_id=%s&redirect_uri=%s"
                + "&scope=openid%20offline%20idverified&state=", clientId,
            redirectUri);
  }

  @Bean
  public ConfigurableJWTProcessor<SecurityContext> getJwtProcessor() {
    ResourceRetriever resourceRetriever =
        new DefaultResourceRetriever(jwtConf.getConnectTimeout(), jwtConf.getReadTimeout());
    URL jwkUrl = getSwanUrl();
    return getContextConfigurableJWTProcessor(resourceRetriever, jwkUrl,
        jwtConf.getRs256());
  }

  public static String getTokenProviderUrl() {
    return oauthUrl + "oauth2/token";
  }

  public static String getOauthUrl() {
    return oauthUrl;
  }

  private URL getSwanUrl() {
    try {
      return new URL(String.format(getOauthUrl() + ".well-known/jwks.json"));
    } catch (MalformedURLException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}


