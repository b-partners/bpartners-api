package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenClaims.BRIDGE_AUTHORITY;

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
public class BridgeScopeFilter extends OncePerRequestFilter {
  private final RequestMatcher bridgeEndpoints;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (isBridgeScoped() && !bridgeEndpoints.matches(request)) {
      throw new AccessDeniedException("Bridge-scoped token is limited to bridge endpoints");
    }
    chain.doFilter(request, response);
  }

  private static boolean isBridgeScoped() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> BRIDGE_AUTHORITY.equals(authority.getAuthority()));
  }
}
