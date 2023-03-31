package app.bpartners.api.repository.bridge.repository;

import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;

public interface BridgeBankRepository {
  BridgeBank findById(Integer id);

  String initiateBankConnection(String email);

}
