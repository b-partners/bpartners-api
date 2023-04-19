package app.bpartners.api.repository.bridge.repository;

import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;

public interface BridgeUserRepository {
  BridgeUser createUser(CreateBridgeUser user);
}
