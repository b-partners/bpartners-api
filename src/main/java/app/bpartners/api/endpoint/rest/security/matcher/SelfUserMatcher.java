package app.bpartners.api.endpoint.rest.security.matcher;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class SelfUserMatcher extends SelfMatcher {
  public SelfUserMatcher(
      HttpMethod method, String antPattern, AuthenticatedResourceProvider authResourceProvider) {
    super(method, antPattern, authResourceProvider);
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    AntPathRequestMatcher antMatcher = new AntPathRequestMatcher(antPattern, method.toString());
    if (!antMatcher.matches(request)) {
      return false;
    }
    return Objects.equals(getId(request), authResourceProvider.getUser().getId());
  }
}
