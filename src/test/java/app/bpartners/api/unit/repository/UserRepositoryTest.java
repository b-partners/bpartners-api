package app.bpartners.api.unit.repository;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_EMAIL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.implementation.UserRepositoryImpl;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserRepositoryTest {
  UserJpaRepository userJpaRepositoryMock;
  UserMapper userMapperMock;
  UserRepositoryImpl subject;
  AccountHolderJpaRepository accountHolderJpaRepositoryMock;
  AccountJpaRepository accountJpaRepositoryMock;
  EntityManager entityManagerMock;
  StripePaymentMethodService stripePaymentMethodServiceMock = mock();

  @BeforeEach
  void setUp() {
    userJpaRepositoryMock = mock(UserJpaRepository.class);
    userMapperMock = mock(UserMapper.class);
    accountHolderJpaRepositoryMock = mock(AccountHolderJpaRepository.class);
    accountJpaRepositoryMock = mock(AccountJpaRepository.class);
    entityManagerMock = mock(EntityManager.class);
    subject =
        new UserRepositoryImpl(
            userJpaRepositoryMock,
            userMapperMock,
            accountHolderJpaRepositoryMock,
            accountJpaRepositoryMock,
            entityManagerMock,
            stripePaymentMethodServiceMock);

    when(userJpaRepositoryMock.save(any())).thenReturn(user());
    when(userJpaRepositoryMock.findByEmail(JOE_EMAIL)).thenReturn(Optional.ofNullable(user()));
    when(userMapperMock.toDomain(any(HUser.class))).thenReturn(expectedUser());
  }

  @Test
  void get_enabled_users_without_subscription() {
    when(userJpaRepositoryMock.getEnabledUsersWithoutSubscription()).thenReturn(List.of(user()));

    var actual = subject.getActiveUsersWithNullSubscription();

    assertEquals(1, actual.size());
    assertTrue(actual.contains(expectedUser()));
  }

  HUser user() {
    return HUser.builder()
        .id(JOE_DOE_ID)
        .phoneNumber("+33 5 12 56 45")
        .status(ENABLED)
        .logoFileId("logo.pdf")
        .build();
  }

  User expectedUser() {
    return User.builder()
        .firstName("Joe")
        .lastName("Doe")
        .mobilePhoneNumber(user().getPhoneNumber())
        .status(user().getStatus())
        .logoFileId(user().getLogoFileId())
        .build();
  }
}
