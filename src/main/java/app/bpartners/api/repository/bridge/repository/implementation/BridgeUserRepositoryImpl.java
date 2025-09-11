package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.bridge.repository.BridgeUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BridgeUserRepositoryImpl implements BridgeUserRepository {
  private final BridgeApi bridgeApi;

  @Override
  public BridgeUser createUser(CreateBridgeUser user) {
    return bridgeApi.createUser(user);
  }
}
