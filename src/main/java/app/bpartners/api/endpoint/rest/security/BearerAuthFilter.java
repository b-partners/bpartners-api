package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;
import static app.bpartners.api.service.utils.SecurityUtils.BEARER_PREFIX;
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
  private static final String API_KEY_QUERY_PARAMETER_NAME = "apiKey";
  private static final String BEARER_QUERY_PARAMETER_NAME = "accessToken";
  private final String authHeader;

  protected BearerAuthFilter(RequestMatcher requestMatcher, String authHeader) {
    super(requestMatcher);
    this.authHeader = authHeader;
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse response) {
    String bearer = request.getHeader(authHeader);
    try {
      if (bearer == null && verifyAntMatcher(request)) {
        String accessToken = request.getParameterMap().get(BEARER_QUERY_PARAMETER_NAME)[0];
        bearer = BEARER_PREFIX + accessToken;
      }
      return getAuthenticationManager()
          .authenticate(new UsernamePasswordAuthenticationToken(bearer, bearer));
    } catch (Exception ignored) {
      String apiKey = request.getHeader(API_KEY_HEADER);
      if (apiKey == null && verifyAntMatcher(request)) {
        apiKey = request.getParameterMap().get(API_KEY_QUERY_PARAMETER_NAME)[0];
      }
      return getAuthenticationManager()
          .authenticate(new UsernamePasswordAuthenticationToken(API_KEY_HEADER, apiKey));
    }
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

  private boolean verifyAntMatcher(HttpServletRequest request) {
    return new AntPathRequestMatcher("/accounts/*/files/*/raw", GET.name()).matches(request);
  }
}
