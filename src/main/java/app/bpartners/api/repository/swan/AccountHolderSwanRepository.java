package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import java.util.List;

public interface AccountHolderSwanRepository {
  List<SwanAccountHolder> getAccountHolders();
}
