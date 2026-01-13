package app.bpartners.api.unit.repository;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.VALID_IDENTITY;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_EMAIL;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.implementation.UserRepositoryImpl;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import com.stripe.model.PaymentMethod;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserRepositoryTest {
  UserJpaRepository userJpaRepositoryMock;
  UserMapper userMapperMock;
  CognitoComponent cognitoComponentMock;
  UserRepositoryImpl subject;
  AccountHolderJpaRepository accountHolderJpaRepositoryMock;
  AccountJpaRepository accountJpaRepositoryMock;
  BankRepository bankRepositoryMock;
  EntityManager entityManagerMock;
  StripePaymentMethodService stripePaymentMethodServiceMock = mock();

  @BeforeEach
  void setUp() {
    userJpaRepositoryMock = mock(UserJpaRepository.class);
    userMapperMock = mock(UserMapper.class);
    cognitoComponentMock = mock(CognitoComponent.class);
    accountHolderJpaRepositoryMock = mock(AccountHolderJpaRepository.class);
    accountJpaRepositoryMock = mock(AccountJpaRepository.class);
    bankRepositoryMock = mock(BankRepository.class);
    entityManagerMock = mock(EntityManager.class);
    subject =
        new UserRepositoryImpl(
            userJpaRepositoryMock,
            userMapperMock,
            cognitoComponentMock,
            accountHolderJpaRepositoryMock,
            accountJpaRepositoryMock,
            bankRepositoryMock,
            entityManagerMock,
            stripePaymentMethodServiceMock);

    when(userJpaRepositoryMock.save(any())).thenReturn(user());
    when(userJpaRepositoryMock.findByEmail(JOE_EMAIL)).thenReturn(Optional.ofNullable(user()));
    when(userMapperMock.toDomain(any(HUser.class))).thenReturn(expectedUser());
    when(cognitoComponentMock.getEmailByToken(JOE_DOE_TOKEN)).thenReturn(JOE_EMAIL);
  }

  @Test
  void get_enabled_users_without_subscription() {
    when(userJpaRepositoryMock.getEnabledUsersWithoutSubscription()).thenReturn(List.of(user()));

    var actual = subject.getActiveUsersWithNullSubscription();

    assertEquals(1, actual.size());
    assertTrue(actual.contains(expectedUser()));
  }

  @Test
  void read_user_by_token_with_computed_payment_method_attribute() {
    var token = randomUUID().toString();
    var email = "random-" + randomUUID() + "@email.com";
    var userSubscriptionId = randomUUID().toString();

    reset(userJpaRepositoryMock, cognitoComponentMock, userMapperMock);
    when(userJpaRepositoryMock.findByAccessToken(token)).thenReturn(Optional.empty());
    when(cognitoComponentMock.getEmailByToken(token)).thenReturn(email);
    when(userJpaRepositoryMock.findByEmail(email)).thenReturn(Optional.of(mock(HUser.class)));
    when(userMapperMock.toDomain(any(HUser.class)))
        .thenReturn(User.builder().userSubscriptionId(userSubscriptionId).build());
    when(stripePaymentMethodServiceMock.getPaymentMethod(userSubscriptionId))
        .thenReturn(List.of(mock(PaymentMethod.class)));

    var actual = subject.getUserByToken(token);

    assertEquals(
        User.builder().userSubscriptionId(userSubscriptionId).paymentMethodExists(true).build(),
        actual);
  }

  @Test
  void read_user_by_token_without_user_subscription_id() {
    var token = randomUUID().toString();
    var email = "random-" + randomUUID() + "@email.com";

    reset(userJpaRepositoryMock, cognitoComponentMock, userMapperMock);
    when(userJpaRepositoryMock.findByAccessToken(token)).thenReturn(Optional.empty());
    when(cognitoComponentMock.getEmailByToken(token)).thenReturn(email);
    when(userJpaRepositoryMock.findByEmail(email)).thenReturn(Optional.of(mock(HUser.class)));
    when(userMapperMock.toDomain(any(HUser.class)))
        .thenReturn(User.builder().userSubscriptionId(null).build());

    var actual = subject.getUserByToken(token);

    verify(stripePaymentMethodServiceMock, never()).getPaymentMethod(any());
    assertEquals(
        User.builder().userSubscriptionId(null).paymentMethodExists(false).build(), actual);
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
