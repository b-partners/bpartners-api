package app.bpartners.api.endpoint.rest.security.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class DashboardTokenVerifierTest {
  private static final String SIGNING_KEY = "test-dashboard-token-signing-key-0123456789";
  private static final String OTHER_KEY = "another-dashboard-token-signing-key-987654";
  private static final String USER_ID = "user-id";

  DashboardTokenVerifier verifier =
      new DashboardTokenVerifier(new DashboardTokenConf(SIGNING_KEY, 15));

  @Test
  void valid_token_yields_api_bridge_principal_with_user_id() throws Exception {
    var token = signedToken(DashboardTokenClaims.ISSUER, SIGNING_KEY, Duration.ofMinutes(5));

    var principal = verifier.verify(token);

    assertEquals(USER_ID, principal.getUserId());
    assertTrue(
        principal.getAuthorities().stream()
            .anyMatch(a -> DashboardTokenClaims.API_BRIDGE_AUTHORITY.equals(a.getAuthority())));
  }

  @Test
  void expired_token_is_rejected() throws Exception {
    var token = signedToken(DashboardTokenClaims.ISSUER, SIGNING_KEY, Duration.ofMinutes(-5));

    assertThrows(BadCredentialsException.class, () -> verifier.verify(token));
  }

  @Test
  void token_signed_with_another_key_is_rejected() throws Exception {
    var token = signedToken(DashboardTokenClaims.ISSUER, OTHER_KEY, Duration.ofMinutes(5));

    assertThrows(BadCredentialsException.class, () -> verifier.verify(token));
  }

  @Test
  void token_with_another_issuer_is_not_a_dashboard_token() throws Exception {
    var token = signedToken("https://cognito-idp.example", SIGNING_KEY, Duration.ofMinutes(5));

    assertFalse(verifier.isIssuedByDashboard(token));
  }

  @Test
  void dashboard_issuer_is_recognized_even_with_bad_signature() throws Exception {
    var token = signedToken(DashboardTokenClaims.ISSUER, OTHER_KEY, Duration.ofMinutes(5));

    assertTrue(verifier.isIssuedByDashboard(token));
  }

  @Test
  void malformed_token_is_not_a_dashboard_token() {
    assertFalse(verifier.isIssuedByDashboard("not-a-jwt"));
  }

  private static String signedToken(String issuer, String key, Duration validity)
      throws JOSEException {
    var claims =
        new JWTClaimsSet.Builder()
            .subject(USER_ID)
            .issuer(issuer)
            .expirationTime(Date.from(Instant.now().plus(validity)))
            .claim(DashboardTokenClaims.SCOPE_CLAIM, DashboardTokenClaims.API_BRIDGE_SCOPE)
            .build();
    var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(key.getBytes(StandardCharsets.UTF_8)));
    return jwt.serialize();
  }
}
