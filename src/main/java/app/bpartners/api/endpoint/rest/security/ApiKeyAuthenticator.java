package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.service.UserService;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ApiKeyAuthenticator implements UsernamePasswordAuthenticator {
  private final UserService userService;

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authenticationToken) {
    var apiKey = getApiKeyFromHeader(authenticationToken);
    var user = userService.getUserByApiKey(apiKey);
    return new Principal(user, apiKey);
  }

  private String getApiKeyFromHeader(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String)
        || !Objects.equals(usernamePasswordAuthenticationToken.getName(), API_KEY_HEADER)) {
      return null;
    }
    return ((String) tokenObject);
  }
}
