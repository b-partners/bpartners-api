package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.schema.SwanUser;

public interface UserSwanRepository {

  SwanUser whoami();

}
