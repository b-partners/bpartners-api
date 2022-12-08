package app.bpartners.api.endpoint.rest.security.matcher;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@AllArgsConstructor
public class SelfUserAccountMatcher implements RequestMatcher {

  private static final Pattern SELFABLE_URI_PATTERN =
      // /user/id/account/id/...
      Pattern.compile("/[^/]+/(?<userId>[^/]+)/[^/]+/(?<accountId>[^/]+)(/.*)?");
  private final HttpMethod method;
  private final String antPattern;
  private final AuthenticatedResourceProvider authResourceProvider;

  @Override
  public boolean matches(HttpServletRequest request) {
    AntPathRequestMatcher antMatcher = new AntPathRequestMatcher(antPattern, method.toString());
    if (!antMatcher.matches(request)) {
      return false;
    }
    return Objects.equals(getSelfUserId(request), AuthProvider.getPrincipal().getUserId())
        &&
        Objects.equals(getSelfAccountId(request), authResourceProvider.getAccount().getId());
  }

  private String getSelfUserId(HttpServletRequest request) {
    Matcher uriMatcher = SELFABLE_URI_PATTERN.matcher(request.getRequestURI());
    return uriMatcher.find() ? uriMatcher.group("userId") : null;
  }

  private String getSelfAccountId(HttpServletRequest request) {
    Matcher uriMatcher = SELFABLE_URI_PATTERN.matcher(request.getRequestURI());
    return uriMatcher.find() ? uriMatcher.group("accountId") : null;
  }
}
