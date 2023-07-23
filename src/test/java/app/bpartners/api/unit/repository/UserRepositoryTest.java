package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.bridge.repository.BridgeUserRepository;
import app.bpartners.api.repository.implementation.UserRepositoryImpl;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.VALID_IDENTITY;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRepositoryTest {
  UserJpaRepository userJpaRepositoryMock;
  UserMapper userMapperMock;
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
    cognitoComponentMock = mock(CognitoComponent.class);
    bridgeUserRepositoryMock = mock(BridgeUserRepository.class);
    accountHolderJpaRepositoryMock = mock(AccountHolderJpaRepository.class);
    accountJpaRepositoryMock = mock(AccountJpaRepository.class);
    bankRepositoryMock = mock(BankRepository.class);
    subject =
        new UserRepositoryImpl(userJpaRepositoryMock, userMapperMock, cognitoComponentMock,
            bridgeUserRepositoryMock,
            accountHolderJpaRepositoryMock, accountJpaRepositoryMock, bankRepositoryMock);

    when(userJpaRepositoryMock.save(any())).thenReturn(user());
    when(userJpaRepositoryMock.findByEmail(JOE_EMAIL)).thenReturn(Optional.ofNullable(user()));
    when(userMapperMock.toDomain(any(HUser.class))).thenReturn(expectedUser());
    when(cognitoComponentMock.getEmailByToken(JOE_DOE_TOKEN)).thenReturn(JOE_EMAIL);
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
