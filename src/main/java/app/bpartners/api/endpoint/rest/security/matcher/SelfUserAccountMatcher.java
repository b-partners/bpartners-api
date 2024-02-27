package app.bpartners.api.endpoint.rest.security.matcher;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@AllArgsConstructor
@Slf4j
public class SelfUserAccountMatcher implements RequestMatcher {
  // TODO: before setting up another selfMatcher with multiple group_names,
  // create an abstract class like SelfMatcher
  // to avoid duplicates
  private static final Pattern SELFABLE_URI_PATTERN =
      // /user/id/account/id/...
      Pattern.compile("/[^/]+/(?<userId>[^/]+)/[^/]+/(?<accountId>[^/]+)(/.*)?");
  private final HttpMethod method;
  private final String antPattern;
  private final AuthenticatedResourceProvider authResourceProvider;

  @Override
  public boolean matches(HttpServletRequest request) {
    var antMatcher = new AntPathRequestMatcher(antPattern, method.toString());
    if (!antMatcher.matches(request)) {
      return false;
    }

    var selfUserId = getSelfUserId(request);
    var selfAccountId = getSelfAccountId(request);
    var principalUserId = AuthProvider.getPrincipal().getUserId();
    return Objects.equals(selfUserId, principalUserId)
        && authResourceProvider.getAccounts().stream()
            .anyMatch(account -> account.getId().equals(selfAccountId));
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
