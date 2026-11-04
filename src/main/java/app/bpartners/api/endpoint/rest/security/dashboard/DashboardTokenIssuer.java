package app.bpartners.api.endpoint.rest.security.dashboard;

import static app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenClaims.API_BRIDGE_SCOPE;
import static app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenClaims.ISSUER;
import static app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenClaims.SCOPE_CLAIM;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.temporal.ChronoUnit.SECONDS;

import app.bpartners.api.endpoint.rest.model.DashboardToken;
import app.bpartners.api.model.exception.ApiException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DashboardTokenIssuer {
  private final DashboardTokenConf conf;

  public DashboardToken issue(String userId) {
    Instant issuedAt = Instant.now().truncatedTo(SECONDS);
    Instant expiresAt = issuedAt.plus(conf.getTtl());
    return new DashboardToken()
        .token(sign(claims(userId, issuedAt, expiresAt)))
        .expiresAt(expiresAt);
  }

  private static JWTClaimsSet claims(String userId, Instant issuedAt, Instant expiresAt) {
    return new JWTClaimsSet.Builder()
        .subject(userId)
        .issuer(ISSUER)
        .issueTime(Date.from(issuedAt))
        .expirationTime(Date.from(expiresAt))
        .claim(SCOPE_CLAIM, API_BRIDGE_SCOPE)
        .build();
  }

  private String sign(JWTClaimsSet claims) {
    try {
      var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(new MACSigner(conf.getSigningKey()));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
