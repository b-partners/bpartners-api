package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.model.SwanUser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
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
    Optional<HUser> user = jpaRepository.findById(id);
    if (user.isEmpty()) {
      throw new NotFoundException("User." + id + " does not exist");
    }
    SwanUser swanUser = swanRepository.whoami();
    HUser identifiedUser = jpaRepository.getUserBySwanUserId(swanUser.getId());
    if (!identifiedUser.equals(user.get())) {
      throw new ForbiddenException();
    }
    return userMapper.toDomain(user.get(), swanUser);
  }

  @Override
  public User getUserBySwanUserIdAndToken(String swanUserId, String token) {
    SwanUser swanUser = null;
    try {
      swanUser = swanComponent.getSwanUserByToken(token);
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new ApiException(ApiException.ExceptionType.CLIENT_EXCEPTION, e);
    }
    HUser entityUser = jpaRepository.getUserBySwanUserId(swanUserId);
    return userMapper.toDomain(entityUser, swanUser);
  }

  @Override
  public User getUserByToken(String token) {
    SwanUser swanUser = swanRepository.getByToken(token);
    HUser entityUser = jpaRepository.getUserBySwanUserId(swanUser.getId());
    return userMapper.toDomain(entityUser, swanUser);
  }
}
