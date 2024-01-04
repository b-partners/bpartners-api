package app.bpartners.api.endpoint.rest.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JWTConf {
  private final JWSAlgorithm rs256;
  private final Integer connectTimeout;
  private final Integer readTimeout;

  public JWTConf(
      @Value("${jwt.jwsAlgorithm}") final String rs256,
      @Value("${jwt.connectTimeout}") final Integer connectTimeout,
      @Value("${jwt.readTimeout}") final Integer readTimeout) {
    this.rs256 = new JWSAlgorithm(rs256);
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  public static ConfigurableJWTProcessor<SecurityContext> getContextConfigurableJWTProcessor(
      ResourceRetriever resourceRetriever, URL jwkUrl, JWSAlgorithm rs256) {
    JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(jwkUrl, resourceRetriever);
    ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
    JWSKeySelector<SecurityContext> keySelector =
        new JWSVerificationKeySelector<>(rs256, keySource);
    jwtProcessor.setJWSKeySelector(keySelector);
    return jwtProcessor;
  }

  public Integer getConnectTimeout() {
    return connectTimeout;
  }

  public Integer getReadTimeout() {
    return readTimeout;
  }

  public JWSAlgorithm getRs256() {
    return rs256;
  }
}
