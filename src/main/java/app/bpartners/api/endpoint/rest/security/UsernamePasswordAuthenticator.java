package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public interface UsernamePasswordAuthenticator {
  UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authenticationToken);

  User retrieveUserWithoutLegalFileCheck(HttpServletRequest request);
}
