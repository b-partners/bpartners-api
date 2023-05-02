package app.bpartners.api.repository.bridge.repository;

import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import java.time.Instant;
import java.util.List;

public interface BridgeBankRepository {
  BridgeBank findById(Long id);

  String initiateBankConnection(String email);

  Instant getItemStatusRefreshedAt(Long itemId, String token);

  List<BridgeItem> getBridgeItems();

  String refreshBankConnection(Long itemId, String token);

  boolean deleteItem(Long itemId, String token);
}
