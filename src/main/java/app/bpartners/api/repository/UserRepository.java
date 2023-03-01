package app.bpartners.api.repository;

import app.bpartners.api.model.User;

public interface UserRepository {
  User getUserBySwanUserIdAndToken(String swanUserId, String token);

  User getUserByToken(String token);
}
