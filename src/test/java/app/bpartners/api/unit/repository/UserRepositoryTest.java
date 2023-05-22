package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.bridge.repository.BridgeUserRepository;
import app.bpartners.api.repository.implementation.UserRepositoryImpl;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
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
  UserSwanRepository userSwanRepositoryMock;
  UserJpaRepository userJpaRepositoryMock;
  UserMapper userMapperMock;
  SwanComponent swanComponentMock;
  CognitoComponent cognitoComponentMock;
  UserRepositoryImpl subject;
  BridgeUserRepository bridgeUserRepositoryMock;
  AccountHolderJpaRepository accountHolderJpaRepositoryMock;
  AccountJpaRepository accountJpaRepositoryMock;
  BankRepository bankRepositoryMock;


  @BeforeEach
  void setUp() {
    userJpaRepositoryMock = mock(UserJpaRepository.class);
    userMapperMock = mock(UserMapper.class);
    userSwanRepositoryMock = mock(UserSwanRepository.class);
    swanComponentMock = mock(SwanComponent.class);
    cognitoComponentMock = mock(CognitoComponent.class);
    bridgeUserRepositoryMock = mock(BridgeUserRepository.class);
    accountHolderJpaRepositoryMock = mock(AccountHolderJpaRepository.class);
    accountJpaRepositoryMock = mock(AccountJpaRepository.class);
    bankRepositoryMock = mock(BankRepository.class);
    subject =
        new UserRepositoryImpl(
            userSwanRepositoryMock, userJpaRepositoryMock, userMapperMock,
            swanComponentMock, cognitoComponentMock, bridgeUserRepositoryMock,
            accountHolderJpaRepositoryMock, accountJpaRepositoryMock, bankRepositoryMock);

    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpSwanComponent(swanComponentMock);
    when(userJpaRepositoryMock.findUserBySwanUserId(any(String.class))).thenReturn(
        Optional.of(user()));
    when(userJpaRepositoryMock.save(any())).thenReturn(user());
    when(userMapperMock.toDomain(any(HUser.class), any(SwanUser.class))).thenReturn(expectedUser());
    when(userMapperMock.toDomain(any(HUser.class))).thenReturn(expectedUser());
  }

  @Test
  void read_user_by_swan_userId_and_token() {
    User actual = subject.getUserBySwanUserIdAndToken(JOE_DOE_SWAN_USER_ID, JOE_DOE_TOKEN);

    assertEquals(expectedUser(), actual);
  }

  @Test
  void read_user_by_token() {
    User actual = subject.getUserByToken(JOE_DOE_TOKEN);

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
