package app.bpartners.api.endpoint.rest.security.principal;

import org.springframework.security.core.Authentication;

public interface PrincipalProvider {
  Authentication getAuthentication();
}
