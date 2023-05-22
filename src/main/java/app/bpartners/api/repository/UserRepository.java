package app.bpartners.api.repository;

import app.bpartners.api.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
  List<User> findAll();

  User getUserBySwanUserIdAndToken(String swanUserId, String token);

  User getUserByToken(String token);

  User getByEmail(String email);

  Optional<User> findByEmail(String email);

  User getById(String id);

  User save(User toSave);

  User create(User user);
}
