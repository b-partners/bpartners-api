package app.bpartners.api.endpoint.rest.security.dashboard;

import static app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenClaims.API_BRIDGE_SCOPE;
import static app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenClaims.ISSUER;
import static app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenClaims.SCOPE_CLAIM;

import app.bpartners.api.endpoint.rest.security.model.ApiBridgePrincipal;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DashboardTokenVerifier {
  private final DashboardTokenConf conf;

  public boolean isIssuedByDashboard(String bearer) {
    return parse(bearer)
        .map(DashboardTokenVerifier::claimsOf)
        .map(claims -> ISSUER.equals(claims.getIssuer()))
        .orElse(false);
  }

  public Principal verify(String bearer) {
    SignedJWT jwt = parse(bearer).orElseThrow(DashboardTokenVerifier::invalid);
    if (!hasValidSignature(jwt)) {
      throw invalid();
    }
    JWTClaimsSet claims = claimsOf(jwt);
    if (!isUnexpiredApiBridgeToken(claims)) {
      throw invalid();
    }
    return new ApiBridgePrincipal(claims.getSubject(), bearer);
  }

  private boolean hasValidSignature(SignedJWT jwt) {
    try {
      return jwt.verify(new MACVerifier(conf.getSigningKey()));
    } catch (JOSEException e) {
      return false;
    }
  }

  private static boolean isUnexpiredApiBridgeToken(JWTClaimsSet claims) {
    Date expiration = claims.getExpirationTime();
    return ISSUER.equals(claims.getIssuer())
        && claims.getSubject() != null
        && expiration != null
        && expiration.after(new Date())
        && API_BRIDGE_SCOPE.equals(claims.getClaim(SCOPE_CLAIM));
  }

  private static Optional<SignedJWT> parse(String bearer) {
    try {
      return Optional.of(SignedJWT.parse(bearer));
    } catch (ParseException e) {
      return Optional.empty();
    }
  }

  private static JWTClaimsSet claimsOf(SignedJWT jwt) {
    try {
      return jwt.getJWTClaimsSet();
    } catch (ParseException e) {
      throw invalid();
    }
  }

  private static UsernameNotFoundException invalid() {
    return new UsernameNotFoundException("Bad credentials: dashboard token is not valid");
  }
}
