package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BridgeBankRepositoryImpl implements BridgeBankRepository {
  private final BridgeApi bridgeApi;

  @Override
  public BridgeBank findById(Integer id) {
    return bridgeApi.findBankById(id);
  }
}
