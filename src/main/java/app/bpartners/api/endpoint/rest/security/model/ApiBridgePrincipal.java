package app.bpartners.api.endpoint.rest.security.model;

import static app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenClaims.API_BRIDGE_AUTHORITY;

import app.bpartners.api.model.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ApiBridgePrincipal extends Principal {
  public ApiBridgePrincipal(String userId, String bearer) {
    super(User.builder().id(userId).build(), bearer);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(API_BRIDGE_AUTHORITY));
  }
}
