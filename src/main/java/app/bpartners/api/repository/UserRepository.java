package app.bpartners.api.repository;

import app.bpartners.api.model.User;

public interface UserRepository {
  User getUserById(String id);

  //TODO: why the need to filter by token?
  User getUserBySwanUserIdAndToken(String swanUserId, String token);

  User getUserByToken(String token);
}
