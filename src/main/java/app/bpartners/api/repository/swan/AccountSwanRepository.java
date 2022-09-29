package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.model.SwanAccount;
import java.util.List;

public interface AccountSwanRepository {
  List<SwanAccount> findAll();

  List<SwanAccount> findByBearer(String bearer);

  List<SwanAccount> findById(String id);

  List<SwanAccount> findByUserId(String userId);
}
