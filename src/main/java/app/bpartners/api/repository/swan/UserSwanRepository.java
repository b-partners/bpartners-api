package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.schema.SwanUser;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSwanRepository {
  public SwanUser getSwanUserById(String id);

  public List<SwanUser> getSwanUsers();
}
