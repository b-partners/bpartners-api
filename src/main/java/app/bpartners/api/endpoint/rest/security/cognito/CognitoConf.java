package app.bpartners.api.endpoint.rest.security.cognito;

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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import static app.bpartners.api.endpoint.rest.security.JWTConf.getContextConfigurableJWTProcessor;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Configuration
public class CognitoConf {

  private final String region;
  private final String userPoolId;
  private final JWTConf jwtConf;

  public CognitoConf(
      @Value("${aws.region}") String region,
      @Value("${aws.cognito.userPool.id}") String userPoolId,
      JWTConf jwtConf) {
    this.region = region;
    this.userPoolId = userPoolId;
    this.jwtConf = jwtConf;
  }

  @Bean
  public ConfigurableJWTProcessor<SecurityContext> getCognitoJwtProcessor() {
    ResourceRetriever resourceRetriever =
        new DefaultResourceRetriever(jwtConf.getConnectTimeout(), jwtConf.getReadTimeout());
    URL jwkUrl = getCognitoJwksUrlFormat();
    return getContextConfigurableJWTProcessor(resourceRetriever, jwkUrl,
        jwtConf.getRs256());
  }

  @Bean
  public CognitoIdentityProviderClient getCognitoClient() {
    return CognitoIdentityProviderClient.builder().region(Region.of(region)).build();
  }

  public String getUserPoolUrl() {
    return String.format("https://cognito-idp.%s.amazonaws.com/%s", region, userPoolId);
  }

  private URL getCognitoJwksUrlFormat() {
    try {
      return new URL(getUserPoolUrl() + "/.well-known/jwks.json");
    } catch (MalformedURLException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public String getUserPoolId() {
    return userPoolId;
  }
}

