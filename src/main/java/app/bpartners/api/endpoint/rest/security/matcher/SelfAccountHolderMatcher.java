package app.bpartners.api.endpoint.rest.security.matcher;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class SelfAccountHolderMatcher extends SelfMatcher {
  public SelfAccountHolderMatcher(
      HttpMethod method, String antPattern, AuthenticatedResourceProvider authResourceProvider) {
    super(method, antPattern, authResourceProvider);
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    AntPathRequestMatcher antMatcher = new AntPathRequestMatcher(antPattern, method.toString());
    if (!antMatcher.matches(request)) {
      return false;
    }
    return authResourceProvider.getAccountHolders().stream()
        .anyMatch(accountHolder -> accountHolder.getId().equals(getId(request)));
  }
}
