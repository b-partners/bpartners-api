package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.model.SwanUser;

public interface UserSwanRepository {

  SwanUser whoami();

  SwanUser getByToken(String token);

}
