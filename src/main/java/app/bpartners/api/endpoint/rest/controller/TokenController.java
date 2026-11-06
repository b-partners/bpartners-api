package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.DashboardToken;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenIssuer;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TokenController {
  private final DashboardTokenIssuer issuer;

  @PostMapping("/token")
  public DashboardToken issueDashboardToken() {
    return issuer.issue(AuthProvider.getAuthenticatedUserId());
  }
}
