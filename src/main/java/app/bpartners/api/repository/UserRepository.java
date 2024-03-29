package app.bpartners.api.repository;

import app.bpartners.api.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

  User getByIdAccount(String idAccount);

  List<User> findAll();

  User getUserByToken(String token);

  User getByEmail(String email);

  Optional<User> findByEmail(String email);

  User getById(String id);

  User save(User toSave);

  User create(User user);
}
