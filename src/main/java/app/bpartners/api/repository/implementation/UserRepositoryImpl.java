package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
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

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Repository
@AllArgsConstructor
public class UserRepositoryImpl implements UserRepository {
  private final UserSwanRepository swanRepository;
  private final UserJpaRepository jpaRepository;
  private final UserMapper userMapper;

  private final SwanComponent swanComponent;

  @Override
  public User getUserBySwanUserIdAndToken(String swanUserId, String token) {
    SwanUser swanUser;
    HUser entityUser;
    try {
      swanUser = swanComponent.getSwanUserByToken(token);
      Optional<HUser> optionalUser = jpaRepository.findUserBySwanUserId(swanUser.getId());
      entityUser = optionalUser.orElseGet(() -> jpaRepository.save(HUser.builder()
          .swanUserId(swanUser.getId())
          .status(ENABLED)
          .monthlySubscription(5) //TODO: change or set default monthly subscription earlier
          .phoneNumber(swanUser.getMobilePhoneNumber())
          .build()));
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(ApiException.ExceptionType.CLIENT_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return userMapper.toDomain(entityUser, swanUser);
  }

  @Override
  public User getUserByToken(String token) {
    SwanUser swanUser = swanRepository.getByToken(token);
    Optional<HUser> optionalUser = jpaRepository.findUserBySwanUserId(swanUser.getId());
    if (optionalUser.isPresent()) {
      return userMapper.toDomain(optionalUser.get(), swanUser);
    }
    HUser newUser = jpaRepository.save(HUser.builder()
        .swanUserId(swanUser.getId())
        .status(ENABLED)
        .monthlySubscription(5) //TODO: change or set default monthly subscription earlier
        .phoneNumber(swanUser.getMobilePhoneNumber())
        .build());
    return userMapper.toDomain(newUser, swanUser);
  }
}
