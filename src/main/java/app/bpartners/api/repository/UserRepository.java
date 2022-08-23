package app.bpartners.api.repository;

import app.bpartners.api.model.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository {
  User getUserById(String id);

  User getUserBySwanUserId(String swanUserId);

  User getUserBySwanUserIdAndToken(String swanUserId, String token);
}
