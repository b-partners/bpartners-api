package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.implementation.UserRepositoryImpl;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.model.SwanUser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.VALID_IDENTITY;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_SWAN_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRepositoryTest {
  UserSwanRepository userSwanRepository;
  UserJpaRepository userJpaRepository;
  UserMapper userMapper;
  SwanComponent swanComponent;
  UserRepositoryImpl userRepository;

  @BeforeEach
  void setUp() {
    userJpaRepository = mock(UserJpaRepository.class);
    userMapper = mock(UserMapper.class);
    userSwanRepository = mock(UserSwanRepository.class);
    swanComponent = mock(SwanComponent.class);
    userRepository =
        new UserRepositoryImpl(userSwanRepository, userJpaRepository, userMapper, swanComponent);

    setUpUserSwanRepository(userSwanRepository);
    setUpSwanComponent(swanComponent);
    when(userJpaRepository.findUserBySwanUserId(any(String.class))).thenReturn(
        Optional.of(user()));
    when(userJpaRepository.save(any())).thenReturn(user());
    when(userMapper.toDomain(any(HUser.class), any(SwanUser.class))).thenReturn(expectedUser());
  }

  @Test
  void read_user_by_swan_userId_and_token() {
    User actual = userRepository.getUserBySwanUserIdAndToken(JOE_DOE_SWAN_USER_ID, JOE_DOE_TOKEN);

    assertEquals(expectedUser(), actual);
  }

  @Test
  void read_user_by_token() {
    User actual = userRepository.getUserByToken(JOE_DOE_TOKEN);

    assertNotNull(actual);
    assertEquals(expectedUser(), actual);
  }

  HUser user() {
    return HUser.builder()
        .id(JOE_DOE_ID)
        .swanUserId(JOE_DOE_SWAN_USER_ID)
        .phoneNumber("+33 5 12 56 45")
        .monthlySubscription(5)
        .status(ENABLED)
        .logoFileId("logo.pdf")
        .build();
  }

  User expectedUser() {
    return User.builder()
        .firstName("Joe")
        .lastName("Doe")
        .mobilePhoneNumber(user().getPhoneNumber())
        .monthlySubscription(user().getMonthlySubscription())
        .identificationStatus(VALID_IDENTITY)
        .idVerified(true)
        .status(user().getStatus())
        .logoFileId(user().getLogoFileId())
        .build();
  }
}
