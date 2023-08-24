package app.bpartners.api.endpoint.rest.security.model;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
  EVAL_PROSPECT;

  public String getRole() {
    return name();
  }

  @Override
  public String getAuthority() {
    return "ROLE_" + getRole();
  }
}
