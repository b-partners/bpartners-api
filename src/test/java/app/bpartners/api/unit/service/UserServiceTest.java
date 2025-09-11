package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserRegistrationRequested;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceSummaryJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.service.SnsService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {
  UserService subject;
  UserRepository userRepositoryMock;
  SnsService snsServiceMock;
  CognitoComponent cognitoComponentMock;
  UserJpaRepository userJpaRepositoryMock;
  AccountJpaRepository accountJpaRepositoryMock;
  AccountHolderJpaRepository accountHolderJpaRepositoryMock;
  InvoiceSummaryJpaRepository invoiceSummaryJpaRepositoryMock;
  EventProducer<UserRegistrationRequested> eventProducerMock;
  SesService mailerMock;
  SubscriptionService subscriptionServiceMock;

  @BeforeEach
  void setUp() {
    userRepositoryMock = mock(UserRepository.class);
    snsServiceMock = mock(SnsService.class);
    subscriptionServiceMock = mock(SubscriptionService.class);
    mailerMock = mock(SesService.class);
    eventProducerMock = mock(EventProducer.class);
    subject =
        new UserService(
            userRepositoryMock,
            snsServiceMock,
            cognitoComponentMock,
            userJpaRepositoryMock,
            accountJpaRepositoryMock,
            accountHolderJpaRepositoryMock,
            invoiceSummaryJpaRepositoryMock,
            eventProducerMock);

    when(userRepositoryMock.getByEmail(any())).thenReturn(user());
    when(userRepositoryMock.getUserByToken(any())).thenReturn(user());
  }

  @Test
  void register_on_stripe_active_users_when_user_has_subscription() {
    var account = AccountHolder.builder().build();
    var user =
        User.builder().id("id_user").accountHolders(List.of(account)).status(ENABLED).build();
    var subscription = Subscription.builder().id("user_subscription_id").build();
    var userSubscription = UserSubscription.builder().subscriptions(List.of(subscription)).build();
    when(userRepositoryMock.getActiveUsersWithNullSubscription()).thenReturn(List.of(user));
    when(subscriptionServiceMock.createOrLinkUserSubscription(any()))
        .thenReturn(UserSubscription.builder().build());
    when(subscriptionServiceMock.getSubscriptionByUser(any())).thenReturn(userSubscription);

    assertDoesNotThrow(() -> subject.registerOnStripeActiveUsersWithNullSubscription());
  }

  @Test
  void register_on_stripe_active_users_with_null_subscription() {
    var account = AccountHolder.builder().build();
    var user =
        User.builder().id("id_user").accountHolders(List.of(account)).status(ENABLED).build();
    when(userRepositoryMock.getActiveUsersWithNullSubscription()).thenReturn(List.of(user));
    when(subscriptionServiceMock.createOrLinkUserSubscription(any()))
        .thenReturn(UserSubscription.builder().build());

    assertDoesNotThrow(() -> subject.registerOnStripeActiveUsersWithNullSubscription());
    verify(eventProducerMock, times(1)).accept(any());
  }

  @Test
  void register_device_ok() {
    when(userRepositoryMock.getById(any())).thenReturn(user());
    when(userRepositoryMock.save(any())).thenReturn(user());

    assertEquals(user(), subject.registerDevice(USER1_ID, JANE_DOE_TOKEN));
  }

  @Test
  void register_device_with_actual_token_ok() {
    var deviceToken = "DEVICE_TOKEN";

    when(userRepositoryMock.getById(any())).thenReturn(user());

    assertEquals(user(), subject.registerDevice(USER1_ID, deviceToken));
  }

  @Test
  void read_user_ok() {
    User userFromEmail = subject.getUserByEmail(user().getEmail());
    User userFromToken = subject.getUserByToken(user().getAccessToken());

    assertNotNull(userFromEmail);
    assertNotNull(userFromToken);
  }

  @Test
  void change_active_account_equals_default_account_ok() {
    var user = mock(User.class);
    var defaultAccount = mock(Account.class);

    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(user.getDefaultAccount()).thenReturn(defaultAccount);
    when(defaultAccount.getId()).thenReturn(JOE_DOE_ACCOUNT_ID);

    var actual = subject.changeActiveAccount(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);
    assertEquals(user, actual);
  }

  @Test
  void change_active_account_not_found_exception() {
    var user = mock(User.class);
    var defaultAccount = mock(Account.class);
    var accounts = mock(List.class);
    var defaultAccountId = "default_account_id";

    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(user.getDefaultAccount()).thenReturn(defaultAccount);
    when(user.getAccounts()).thenReturn(accounts);
    when(accounts.get(anyInt())).thenReturn(defaultAccount);
    when(defaultAccount.getId()).thenReturn(defaultAccountId);

    assertThrows(
        NotFoundException.class,
        () -> {
          subject.changeActiveAccount(JOE_DOE_USER_ID, JANE_ACCOUNT_ID);
        });
  }

  @Test
  void change_active_account_ok() {
    var user = mock(User.class);
    var defaultAccount = mock(Account.class);
    var accounts = mock(List.class);

    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(user.getDefaultAccount()).thenReturn(defaultAccount);
    when(defaultAccount.getId()).thenReturn(JOE_DOE_ACCOUNT_ID);
    when(user.getAccounts()).thenReturn(accounts);
    when(accounts.get(anyInt())).thenReturn(defaultAccount);
    when(userRepositoryMock.save(any())).thenReturn(user());

    var actual = subject.changeActiveAccount(USER1_ID, JOE_DOE_ACCOUNT_ID);
    assertEquals(user, actual);
  }

  User user() {
    return User.builder()
        .id(JOE_DOE_ID)
        .email("exemple@gmail.com")
        .accessToken(JOE_DOE_TOKEN)
        .deviceToken("DEVICE_TOKEN")
        .build();
  }
}
