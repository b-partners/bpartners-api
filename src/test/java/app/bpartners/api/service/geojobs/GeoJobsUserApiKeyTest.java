package app.bpartners.api.service.geojobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.user.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class GeoJobsUserApiKeyTest {
  private static final String USER_ID = "user-1";

  UserService userService = mock();
  GeoJobsUserApiKey subject = new GeoJobsUserApiKey(userService);

  @BeforeEach
  void authenticate_user() {
    var principal = new Principal(User.builder().id(USER_ID).build(), "bearer");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, "bearer"));
  }

  @AfterEach
  void clear_authentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticated_user_own_enabled_key_is_used() {
    when(userService.getAnalysisApiKeys(USER_ID)).thenReturn(List.of(key("user-key", true, null)));

    assertEquals("user-key", subject.ofAuthenticatedUser());
  }

  @Test
  void disabled_and_expired_keys_are_skipped() {
    var expiredAt = Instant.now().minus(1, ChronoUnit.DAYS);
    var expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);
    when(userService.getAnalysisApiKeys(USER_ID))
        .thenReturn(
            List.of(
                key("disabled-key", false, null),
                key("expired-key", true, expiredAt),
                key("valid-key", true, expiresAt)));

    assertEquals("valid-key", subject.ofAuthenticatedUser());
  }

  @Test
  void user_without_usable_key_is_forbidden() {
    when(userService.getAnalysisApiKeys(USER_ID))
        .thenReturn(List.of(key("disabled-key", false, null)));

    assertThrows(ForbiddenException.class, () -> subject.ofAuthenticatedUser());
  }

  private static UserAnalysisApiKey key(String apiKey, boolean enabled, Instant expiration) {
    return UserAnalysisApiKey.builder()
        .apiKey(apiKey)
        .enabled(enabled)
        .expirationDatetime(expiration)
        .build();
  }
}
