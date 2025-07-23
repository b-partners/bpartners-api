package app.bpartners.api.repository.bridge.repository;

import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import java.util.List;

public interface BridgeBankRepository {
  BridgeBank findById(Long id);

  List<BridgeItem> getBridgeItems();
}
