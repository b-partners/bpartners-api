package app.bpartners.api.endpoint.rest.security.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DashboardTokenIssuerTest {
  private static final String SIGNING_KEY = "test-dashboard-token-signing-key-0123456789";
  private static final String USER_ID = "user-id";

  DashboardTokenConf conf = new DashboardTokenConf(SIGNING_KEY, 7);
  DashboardTokenIssuer issuer = new DashboardTokenIssuer(conf);

  @Test
  void issued_token_carries_user_issuer_scope_and_ttl_based_expiration() throws Exception {
    var token = issuer.issue(USER_ID);

    var claims = parse(token.getToken()).getJWTClaimsSet();
    assertEquals(USER_ID, claims.getSubject());
    assertEquals(DashboardTokenClaims.ISSUER, claims.getIssuer());
    assertEquals(DashboardTokenClaims.API_BRIDGE_SCOPE, claims.getClaim("scope"));
    assertEquals(
        conf.getTtl().toSeconds(),
        claims.getExpirationTime().toInstant().getEpochSecond()
            - claims.getIssueTime().toInstant().getEpochSecond());
    assertEquals(claims.getExpirationTime().toInstant(), token.getExpiresAt());
    assertTrue(token.getExpiresAt().isAfter(Instant.now()));
  }

  @Test
  void issued_token_is_signed_with_configured_key() throws Exception {
    var jwt = parse(issuer.issue(USER_ID).getToken());

    assertTrue(jwt.verify(new MACVerifier(conf.getSigningKey())));
  }

  private static SignedJWT parse(String token) throws ParseException {
    return SignedJWT.parse(token);
  }
}
