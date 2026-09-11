package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

class BearerAuthFilterTest {
  private static final String BILLING_STATS_PATH = "/subscriptionBillingStats";
  AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

  private BearerAuthFilter filter() {
    var filter = new BearerAuthFilter(AnyRequestMatcher.INSTANCE, "Authorization");
    filter.setAuthenticationManager(authenticationManager);
    return filter;
  }

  @Test
  void missing_credentials_yield_authentication_exception_not_npe() {
    when(authenticationManager.authenticate(any()))
        .thenThrow(new BadCredentialsException("bad credentials"));
    var request = new MockHttpServletRequest("GET", BILLING_STATS_PATH);

    assertThrows(
        AuthenticationException.class,
        () -> filter().attemptAuthentication(request, new MockHttpServletResponse()));
    verify(authenticationManager, times(2)).authenticate(any());
  }

  @Test
  void api_key_query_parameter_authenticates() {
    Authentication authenticated =
        new UsernamePasswordAuthenticationToken("admin", "secret", List.of());
    when(authenticationManager.authenticate(any()))
        .thenAnswer(
            invocation -> {
              UsernamePasswordAuthenticationToken token = invocation.getArgument(0);
              if (API_KEY_HEADER.equals(token.getPrincipal())) {
                return authenticated;
              }
              throw new BadCredentialsException("no bearer");
            });
    var request = new MockHttpServletRequest("GET", BILLING_STATS_PATH);
    request.setParameter("apiKey", "secret");

    var actual = filter().attemptAuthentication(request, new MockHttpServletResponse());

    assertSame(authenticated, actual);
  }
}
