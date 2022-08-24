package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.entity.HUser;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.schema.SwanUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class UserRepositoryImpl implements UserRepository {
  private final UserSwanRepository swanRepository;
  private final UserJpaRepository jpaRepository;
  private final UserMapper userMapper;

  private final SwanComponent swanComponent;

  @Override
  public User getUserById(String id) {
    HUser user = jpaRepository.getById(id);
    SwanUser swanUser = swanRepository.whoami();
    return userMapper.toDomain(user, swanUser);
  }

  @Override
  public User getUserBySwanUserId(String swanUserId) {
    HUser hUser = jpaRepository.getUserBySwanUserId(swanUserId);
    SwanUser swanUser = swanRepository.whoami();
    return userMapper.toDomain(hUser, swanUser);
  }

  @Override
  public User getUserBySwanUserIdAndToken(String swanUserId, String token) {
    SwanUser swanUser = swanComponent.getSwanUserByToken(token);
    HUser hUser = jpaRepository.getUserBySwanUserId(swanUserId);
    return userMapper.toDomain(hUser, swanUser);
  }
}
