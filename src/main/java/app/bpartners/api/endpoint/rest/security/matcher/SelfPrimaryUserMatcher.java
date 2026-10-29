package app.bpartners.api.endpoint.rest.security.matcher;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.service.subscription.PrimaryUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class SelfPrimaryUserMatcher extends SelfMatcher {
  private final PrimaryUserResolver primaryUserResolver;

  public SelfPrimaryUserMatcher(
      HttpMethod method,
      String antPattern,
      AuthenticatedResourceProvider authResourceProvider,
      PrimaryUserResolver primaryUserResolver) {
    super(method, antPattern, authResourceProvider);
    this.primaryUserResolver = primaryUserResolver;
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    AntPathRequestMatcher antMatcher = new AntPathRequestMatcher(antPattern, method.toString());
    if (!antMatcher.matches(request)) {
      return false;
    }
    var authenticatedUserId = authResourceProvider.getUser().getId();
    return Objects.equals(getId(request), authenticatedUserId)
        && !primaryUserResolver.isSecondary(authenticatedUserId);
  }
}
