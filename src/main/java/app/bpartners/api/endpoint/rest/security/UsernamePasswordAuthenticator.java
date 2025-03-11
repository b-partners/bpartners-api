package app.bpartners.api.endpoint.rest.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public interface UsernamePasswordAuthenticator {
  UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authenticationToken);
}
