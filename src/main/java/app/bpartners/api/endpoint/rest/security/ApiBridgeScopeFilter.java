package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenClaims.API_BRIDGE_AUTHORITY;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@AllArgsConstructor
public class ApiBridgeScopeFilter extends OncePerRequestFilter {
  private final RequestMatcher apiBridgeEndpoints;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (isApiBridgeScoped() && !apiBridgeEndpoints.matches(request)) {
      throw new AccessDeniedException("Api-bridge-scoped token is limited to api bridge endpoints");
    }
    chain.doFilter(request, response);
  }

  private static boolean isApiBridgeScoped() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> API_BRIDGE_AUTHORITY.equals(authority.getAuthority()));
  }
}
