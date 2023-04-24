package app.bpartners.api.repository;

import app.bpartners.api.model.User;
import java.util.List;

public interface UserRepository {
  List<User> findAll();

  User getUserBySwanUserIdAndToken(String swanUserId, String token);

  User getUserByToken(String token);

  User getByEmail(String email);

  User save(User toSave);
}
