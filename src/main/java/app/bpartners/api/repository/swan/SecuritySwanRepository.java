package app.bpartners.api.repository.swan;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.Token;
import org.springframework.stereotype.Repository;

@Repository
public interface SecuritySwanRepository {
  public Token generateToken();

  public Redirection redirectToAuth();
}
