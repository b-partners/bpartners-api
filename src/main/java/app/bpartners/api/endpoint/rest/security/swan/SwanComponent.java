package app.bpartners.api.endpoint.rest.security.swan;

import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.model.exception.NotImplementedException;
import org.springframework.stereotype.Component;

@Component
public class SwanComponent {

  public String getSwanUserIdByToken(String accessToken) {
    return getUserInfos().getSwanId();
  }

  public SwanUser getUserById(String swanUserId) {
    throw new NotImplementedException("GraphQL API client not yet configured");
  }

  public SwanUser getUserInfos() {
    throw new NotImplementedException("GraphQL API client not yet configured");
  }
}
