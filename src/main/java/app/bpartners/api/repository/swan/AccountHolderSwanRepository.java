package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import java.util.List;

public interface AccountHolderSwanRepository {
  List<SwanAccountHolder> findAllByAccountId(String accountId);

  List<SwanAccountHolder> findAllByBearerAndAccountId(String bearer, String accountId);

  SwanAccountHolder getById(String id);
}
