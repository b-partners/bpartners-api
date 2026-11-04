package app.bpartners.api.service.geojobs;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.user.UserService;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GeoJobsUserApiKey {
  private final UserService userService;

  public String ofAuthenticatedUser() {
    return userService.getAnalysisApiKeys(AuthProvider.getAuthenticatedUserId()).stream()
        .filter(GeoJobsUserApiKey::isUsable)
        .map(UserAnalysisApiKey::getApiKey)
        .findFirst()
        .orElseThrow(
            () -> new ForbiddenException("Authenticated user has no active analysis api key"));
  }

  private static boolean isUsable(UserAnalysisApiKey apiKey) {
    var expiration = apiKey.getExpirationDatetime();
    return apiKey.isEnabled() && (expiration == null || expiration.isAfter(Instant.now()));
  }
}
