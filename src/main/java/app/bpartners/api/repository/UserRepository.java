package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

  User getByIdAccount(String idAccount);

  List<User> getActiveUsersWithNullSubscription();

  List<User> findAll();

  Long countUsersByStatus(EnableStatus status);

  List<User> findAllByCriteria(HashMap<String, Object> criteria);

  User getUserByToken(String token);

  User getByEmail(String email);

  Optional<User> findByEmail(String email);

  User getById(String id);

  User save(User toSave);

  User create(User user);

  void deleteById(String id);

  List<User> getUsersWithSubscription();

  List<User> findSubUsersByParentId(String id);
}
