package app.bpartners.api.endpoint.rest.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;

import app.bpartners.api.endpoint.rest.security.model.BridgePrincipal;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

class BridgeScopeFilterTest {
  BridgeScopeFilter filter =
      new BridgeScopeFilter(
          new OrRequestMatcher(
              new AntPathRequestMatcher("/llm/toiture"),
              new AntPathRequestMatcher("/geodata/geocode", GET.name()),
              new AntPathRequestMatcher("/geoserver/wms")));

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private static void authenticateAs(Principal principal) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal, principal.getBearer(), principal.getAuthorities()));
  }

  private MockFilterChain filterRequest(String method, String path) throws Exception {
    var chain = new MockFilterChain();
    filter.doFilter(new MockHttpServletRequest(method, path), new MockHttpServletResponse(), chain);
    return chain;
  }

  @Test
  void bridge_scoped_principal_is_accepted_on_bridge_paths() throws Exception {
    authenticateAs(new BridgePrincipal("user-id", "token"));

    assertNotNull(filterRequest("POST", "/llm/toiture").getRequest());
    assertNotNull(filterRequest("GET", "/geodata/geocode").getRequest());
    assertNotNull(filterRequest("GET", "/geoserver/wms").getRequest());
  }

  @Test
  void bridge_scoped_principal_is_refused_elsewhere() {
    authenticateAs(new BridgePrincipal("user-id", "token"));

    assertThrows(AccessDeniedException.class, () -> filterRequest("GET", "/whoami"));
    assertThrows(AccessDeniedException.class, () -> filterRequest("POST", "/token"));
    assertThrows(AccessDeniedException.class, () -> filterRequest("POST", "/geodata/geocode"));
  }

  @Test
  void cognito_principal_is_accepted_everywhere() throws Exception {
    var cognitoUser = User.builder().id("user-id").roles(List.of()).build();
    authenticateAs(new Principal(cognitoUser, "token"));

    assertNotNull(filterRequest("GET", "/whoami").getRequest());
    assertNotNull(filterRequest("POST", "/llm/toiture").getRequest());
  }
}
