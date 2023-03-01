package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
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
  private final CognitoComponent cognitoComponent;

  @Override
  public User getUserBySwanUserIdAndToken(String swanUserId, String token) {
    HUser entityUser;
    SwanUser swanUser;
    try {
      swanUser = swanComponent.getSwanUserByToken(token);
      if (swanUser != null) {
        entityUser = getUpdatedUser(swanUser);
      } else {
        entityUser = jpaRepository.getByPhoneNumber(cognitoComponent.getPhoneNumberByToken(token));
      }
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
    HUser entityUser;
    if (swanUser != null) {
      entityUser = getUpdatedUser(swanUser);
    } else {
      entityUser = jpaRepository.getByPhoneNumber(cognitoComponent.getPhoneNumberByToken(token));
    }
    return userMapper.toDomain(entityUser, swanUser);
  }

  @Override
  public User getByPhoneNumber(String phoneNumber) {
    return userMapper.toDomain(jpaRepository.getByPhoneNumber(phoneNumber), null);
  }

  public HUser getUpdatedUser(SwanUser swanUser) {
    HUser entityUser;
    Optional<HUser> optionalUser = jpaRepository.findUserBySwanUserId(swanUser.getId());
    if (optionalUser.isPresent()) {
      HUser persisted = optionalUser.get();
      if (persisted.getFirstName() == null
          || (!persisted.getFirstName().equals(swanUser.getFirstName()))) {
        persisted.setFirstName(swanUser.getFirstName());
      }
      if (persisted.getLastName() == null
          || (!persisted.getLastName().equals(swanUser.getLastName()))) {
        persisted.setLastName(swanUser.getLastName());
      }
      if (persisted.getIdVerified() == null
          || (!persisted.getIdVerified().equals(swanUser.isIdVerified()))) {
        persisted.setIdVerified(swanUser.isIdVerified());
      }
      if (persisted.getIdentificationStatus() == null
          || (!persisted.getIdentificationStatus().getValue()
          .equals(swanUser.getIdentificationStatus()))) {
        persisted.setIdentificationStatus(
            userMapper.getIdentificationStatus(swanUser.getIdentificationStatus()));
      }
      entityUser = jpaRepository.save(persisted);
    } else {
      entityUser = jpaRepository.save(retrieveUser(swanUser));
    }
    return entityUser;
  }

  private HUser retrieveUser(SwanUser swanUser) {
    return HUser.builder()
        .firstName(swanUser.getFirstName())
        .lastName(swanUser.getLastName())
        .swanUserId(swanUser.getId())
        .status(ENABLED)
        .monthlySubscription(5) //TODO: change or set default monthly subscription earlier
        .idVerified(swanUser.isIdVerified())
        .identificationStatus(
            userMapper.getIdentificationStatus(swanUser.getIdentificationStatus()))
        .phoneNumber(swanUser.getMobilePhoneNumber())
        .build();
  }
}
