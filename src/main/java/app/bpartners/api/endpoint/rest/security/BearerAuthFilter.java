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
        String accessToken = firstParameterValue(request, BEARER_QUERY_PARAMETER_NAME);
        if (accessToken != null) {
          bearer = BEARER_PREFIX + accessToken;
        }
      }
      var bearerToken = new UsernamePasswordAuthenticationToken(bearer, bearer);
      bearerToken.setDetails(request);
      return getAuthenticationManager().authenticate(bearerToken);
    } catch (Exception ignored) {
      String apiKey = request.getHeader(API_KEY_HEADER);
      if (apiKey == null) {
        apiKey = firstCookieValue(request, API_KEY_HEADER);
      }
      var apiKeyToken = new UsernamePasswordAuthenticationToken(API_KEY_HEADER, apiKey);
      apiKeyToken.setDetails(request);
      return getAuthenticationManager().authenticate(apiKeyToken);
    }
  }

  private static String firstParameterValue(HttpServletRequest request, String name) {
    String[] values = request.getParameterMap().get(name);
    return values == null || values.length == 0 ? null : values[0];
  }

  private static String firstCookieValue(HttpServletRequest request, String name) {
    var cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (var cookie : cookies) {
      if (name.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
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
