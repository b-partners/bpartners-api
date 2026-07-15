package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.DASHBOARD;
import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserRegistrationRequested;
import app.bpartners.api.endpoint.rest.model.UserApiKey;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.UserApiKeyMapper;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceSummaryJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.UserService;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserServiceTest {
  UserService subject;
  UserRepository userRepositoryMock;
  CognitoComponent cognitoComponentMock;
  UserJpaRepository userJpaRepositoryMock;
  AccountJpaRepository accountJpaRepositoryMock;
  AccountHolderJpaRepository accountHolderJpaRepositoryMock;
  InvoiceSummaryJpaRepository invoiceSummaryJpaRepositoryMock;
  EventProducer<UserRegistrationRequested> eventProducerMock;
  SesService mailerMock;
  SubscriptionService subscriptionServiceMock;
  UserAnalysisApiKeyRepository analysisApiKeyRepositoryMock;
  UserApiKeyMapper userApiKeyMapper = new UserApiKeyMapper();

  @BeforeEach
  void setUp() {
    userRepositoryMock = mock(UserRepository.class);
    subscriptionServiceMock = mock(SubscriptionService.class);
    mailerMock = mock(SesService.class);
    eventProducerMock = mock(EventProducer.class);
    analysisApiKeyRepositoryMock = mock(UserAnalysisApiKeyRepository.class);
    subject =
        new UserService(
            userRepositoryMock,
            cognitoComponentMock,
            userJpaRepositoryMock,
            accountJpaRepositoryMock,
            accountHolderJpaRepositoryMock,
            invoiceSummaryJpaRepositoryMock,
            eventProducerMock,
            analysisApiKeyRepositoryMock,
            userApiKeyMapper);

    when(userRepositoryMock.getByEmail(any())).thenReturn(user());
  }

  @Test
  void get_enabled_users() {
    var userMock = mock(User.class);
    when(userRepositoryMock.countUsersByStatus(ENABLED)).thenReturn(1L);
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(List.of(userMock));

    var actual = subject.getEnabledUsers();

    assertEquals(List.of(userMock), actual);
    var hashMapCaptor = ArgumentCaptor.forClass(HashMap.class);
    verify(userRepositoryMock).findAllByCriteria(hashMapCaptor.capture());
    var expectedCriteria = new HashMap<String, Object>();
    expectedCriteria.put("status", ENABLED);
    expectedCriteria.put("page", 1);
    expectedCriteria.put("pageSize", MAX_SIZE);
    var actualCriteria = hashMapCaptor.getValue();
    assertEquals(expectedCriteria, actualCriteria);
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
  void read_user_ok() {
    User userFromEmail = subject.getUserByEmail(user().getEmail());

    assertNotNull(userFromEmail);
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
    return User.builder().id(JOE_DOE_ID).email("exemple@gmail.com").build();
  }

  @Test
  void get_api_keys_where_provided_key_types_is_empty_or_is_null() {
    var apiKey = randomUUID().toString();
    var userMock = mock(User.class);
    when(userMock.getApiKey()).thenReturn(apiKey);

    var actualEmptyTypes = subject.getApiKeys(userMock, List.of());
    var actualNullProvidedTypes = subject.getApiKeys(userMock, null);

    assertEquals(
        List.of(new UserApiKey().key(apiKey).type(DASHBOARD).enabled(true)),
        actualNullProvidedTypes);
    assertEquals(
        List.of(new UserApiKey().key(apiKey).type(DASHBOARD).enabled(true)), actualEmptyTypes);
  }

  @Test
  void get_api_keys_where_provided_key_types_is_only_dashboard() {
    var apiKey = randomUUID().toString();
    var userMock = mock(User.class);
    when(userMock.getApiKey()).thenReturn(apiKey);

    var actual = subject.getApiKeys(userMock, List.of(DASHBOARD));

    assertEquals(List.of(new UserApiKey().key(apiKey).type(DASHBOARD).enabled(true)), actual);
    verify(analysisApiKeyRepositoryMock, never()).getAllByUserId(anyString());
  }

  @Test
  void get_api_keys_where_provided_key_types_is_only_analysis() {
    var userId = randomUUID().toString();
    var analysisApiKey = randomUUID().toString();
    var creationDatetime = now();
    var userMock = mock(User.class);

    when(userMock.getId()).thenReturn(userId);
    when(analysisApiKeyRepositoryMock.getAllByUserId(userId))
        .thenReturn(
            List.of(
                UserAnalysisApiKey.builder()
                    .id(randomUUID().toString())
                    .user(userMock)
                    .apiKey(analysisApiKey)
                    .creationDatetime(creationDatetime)
                    .enabled(true)
                    .build()));

    var actual = subject.getApiKeys(userMock, List.of(ANALYSIS));

    assertEquals(
        List.of(
            new UserApiKey()
                .key(userId)
                .type(ANALYSIS)
                .key(analysisApiKey)
                .creationDatetime(creationDatetime)
                .enabled(true)),
        actual);
    verify(userMock, never()).getApiKey();
  }

  @Test
  void get_api_keys_where_provided_key_types_both_dashboard_and_analysis() {
    var userId = randomUUID().toString();
    var analysisApiKey = randomUUID().toString();
    var dashboardApiKey = randomUUID().toString();
    var creationDatetime = now();
    var userMock = mock(User.class);

    when(userMock.getId()).thenReturn(userId);
    when(userMock.getApiKey()).thenReturn(dashboardApiKey);
    when(analysisApiKeyRepositoryMock.getAllByUserId(userId))
        .thenReturn(
            List.of(
                UserAnalysisApiKey.builder()
                    .id(randomUUID().toString())
                    .user(userMock)
                    .apiKey(analysisApiKey)
                    .creationDatetime(creationDatetime)
                    .enabled(true)
                    .build()));

    var actual = subject.getApiKeys(userMock, List.of(DASHBOARD, ANALYSIS));

    assertEquals(
        List.of(
            new UserApiKey().key(dashboardApiKey).type(DASHBOARD).enabled(true),
            new UserApiKey()
                .key(userId)
                .type(ANALYSIS)
                .key(analysisApiKey)
                .creationDatetime(creationDatetime)
                .enabled(true)),
        actual);
  }
}
