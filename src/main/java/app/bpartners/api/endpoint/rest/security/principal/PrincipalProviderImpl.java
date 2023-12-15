package app.bpartners.api.endpoint.rest.security.principal;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class PrincipalProviderImpl implements PrincipalProvider {

  @Override
  public org.springframework.security.core.Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }
}
