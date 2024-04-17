package app.bpartners.api.endpoint.rest.security;

import static org.springframework.http.HttpMethod.GET;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Slf4j
public class BearerAuthFilter extends AbstractAuthenticationProcessingFilter {

  private static final String BEARER_QUERY_PARAMETER_NAME = "accessToken";
  private static final String BEARER_PREFIX = "Bearer ";
  private final String authHeader;
  private final RequestMatcher requiresAuthenticationRequestMatcher;
  static final RequestMatcher accessTokenAccessibleRequestMatcher =
      new AntPathRequestMatcher("/accounts/*/files/*/raw", GET.name());

  protected BearerAuthFilter(
      RequestMatcher requiresAuthenticationRequestMatcher, String authHeader) {
    super(requiresAuthenticationRequestMatcher);
    this.requiresAuthenticationRequestMatcher = requiresAuthenticationRequestMatcher;
    this.authHeader = authHeader;
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse response) {
    String bearer = request.getHeader(authHeader);
    if (bearer == null
        && accessTokenAccessibleRequestMatcher.matches(
            request) /*we can skip verifyAntMatcher as this function only execute if requiresAuthentication is true*/) {
      String accessToken = request.getParameterMap().get(BEARER_QUERY_PARAMETER_NAME)[0];
      bearer = BEARER_PREFIX + accessToken;
    }
    return getAuthenticationManager()
        .authenticate(new UsernamePasswordAuthenticationToken(bearer, bearer));
  }

  @Override
  protected boolean requiresAuthentication(
      HttpServletRequest request, HttpServletResponse response) {
    if (!this.requiresAuthenticationRequestMatcher.matches(request)) {
      return false;
    }
    String authHeaderValue = request.getHeader(authHeader);
    var requestHasAuthHeader = authHeaderValue != null;
    var requestHasAuthParamAndMatchesPath =
        authHeaderValue == null && accessTokenAccessibleRequestMatcher.matches(request);
    return requestHasAuthHeader || requestHasAuthParamAndMatchesPath;
  }

  @Override
  protected void successfulAuthentication(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain,
      Authentication authenticated)
      throws IOException, ServletException {
    super.successfulAuthentication(request, response, chain, authenticated);
    chain.doFilter(request, response);
  }
}
