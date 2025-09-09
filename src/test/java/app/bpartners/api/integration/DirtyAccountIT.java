package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static app.bpartners.api.repository.bridge.model.Account.BridgeAccount.BRIDGE_STATUS_OK;
import static app.bpartners.api.repository.bridge.model.Account.BridgeAccount.BRIDGE_STATUS_SCA;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Account;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.UpdateAccountIdentity;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.model.Item.BridgeConnectItem;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import app.bpartners.api.repository.implementation.BankRepositoryImpl;
import app.bpartners.api.repository.model.AccountConnector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@Disabled("TODO(fail)")
class DirtyAccountIT extends MockedThirdParties {
  @MockBean private UserRepository userRepositoryMock;
  @MockBean private BridgeBankRepository bridgeBankRepositoryMock;
  @MockBean private BankRepositoryImpl bankRepositoryImplMock;
  @Mock private UserTokenRepository userTokenRepositoryMock;
  private static final String OTHER_USER_ID = "OTHER_USER_ID";

  private ApiClient joeDoeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort);
  }

  public static UpdateAccountIdentity bicUpdateOnly() {
    return new UpdateAccountIdentity().name("another name").bic("SWNBFR23").iban("another iban");
  }

  public static UpdateAccountIdentity fullUpdateIdentity() {
    return new UpdateAccountIdentity().name("New name").bic("SWNBFR23").iban("New Iban");
  }

  User joeDoeUser() {
    return User.builder()
        .id(JOE_DOE_ID)
        .email("joe@email.com")
        .preferredAccountId(null)
        .accounts(List.of(joeDoeModelAccount()))
        .roles(List.of())
        .build();
  }

  User bernardUser() {
    return User.builder()
        .id(BERNARD_DOE_ID)
        .email("bernard@email.com")
        .preferredAccountId(null)
        .accounts(List.of(bernardDoeModelAccount()))
        .roles(List.of())
        .build();
  }

  private User userWithPreferredAccount() {
    return User.builder()
        .id(JOE_DOE_ID)
        .preferredAccountId(String.valueOf(otherBridgeAccount().getId()))
        .email("joe@email.com")
        .accounts(List.of(joeDoeModelAccount()))
        .roles(List.of())
        .build();
  }

  app.bpartners.api.model.Account joeDoeModelAccount() {
    return app.bpartners.api.model.Account.builder()
        .id(JOE_DOE_ACCOUNT_ID)
        .userId(JOE_DOE_ID)
        .status(AccountStatus.OPENED)
        .bank(Bank.builder().build())
        .availableBalance(new Money(parseFraction(100000)))
        .active(true)
        .build();
  }

  app.bpartners.api.model.Account bernardDoeModelAccount() {
    return app.bpartners.api.model.Account.builder()
        .id("TODO")
        .name("TODO")
        .iban("TODO")
        .bic("TODO")
        .status(AccountStatus.VALIDATION_REQUIRED)
        .bank(Bank.builder().build())
        .availableBalance(new Money(parseFraction(100000)))
        .active(true)
        .build();
  }

  private void setUpUserRepository(UserRepository userRepositoryMock) {
    when(userRepositoryMock.findAll()).thenReturn(List.of(joeDoeUser()));
    when(userRepositoryMock.getUserByToken(JOE_DOE_TOKEN)).thenReturn(joeDoeUser());
    when(userRepositoryMock.getByEmail(JOE_EMAIL)).thenReturn(joeDoeUser());
    when(userRepositoryMock.getById(JOE_DOE_ID)).thenReturn(joeDoeUser());
    when(userRepositoryMock.getById(JOE_DOE_ID)).thenReturn(joeDoeUser());
    when(userRepositoryMock.findAll()).thenReturn(List.of(joeDoeUser()));
  }

  private void setUpUserBernardRepository(UserRepository userRepositoryMock) {
    when(userRepositoryMock.findAll()).thenReturn(List.of(bernardUser()));
    when(userRepositoryMock.getUserByToken(any())).thenReturn(bernardUser());
    when(userRepositoryMock.getByEmail(any())).thenReturn(bernardUser());
  }

  private void setUpUserRepositoryWithPreferredAccount(UserRepository userRepositoryMock) {
    User user = userWithPreferredAccount();

    when(userRepositoryMock.findAll()).thenReturn(List.of(user));
    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(userRepositoryMock.getUserByToken(any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
  }

  private void setUpUserRepositoryWithoutPreferredAccount(UserRepository userRepositoryMock) {
    User user = userWithPreferredAccount().toBuilder().preferredAccountId(null).build();
    when(userRepositoryMock.findAll()).thenReturn(List.of(user));
    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(userRepositoryMock.getUserByToken(any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
  }

  private void setUpBridgeRepositories() {
    reset(userRepositoryMock);
    setUpUserRepositoryWithPreferredAccount(userRepositoryMock);
    when(bankRepositoryImplMock.findByExternalId(String.valueOf(joeDoeBridgeAccount().getBankId())))
        .thenReturn(new Bank());
    when(bankRepositoryImplMock.disconnectBank(any())).thenReturn(true);
  }

  @BeforeEach
  void setUp() {
    setUpUserRepository(userRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);
  }

  // TODO: add read accounts by user ID ok

  /*
  @Test
  void disconnect_bank_ok() throws ApiException {
    setUpBridgeRepositories();
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);
    Account beforeDisconnection = api.getAccountsByUserId(JOE_DOE_ID).get(0);

    api.disconnectBank(JOE_DOE_ID);
    reset(bridgeApi);
    reset(userRepositoryMock);
    User user = User.builder()
        .id(JOE_DOE_ID)
        .email("joe@email.com")
        .accounts(List.of(joeDoeModelAccount()))
        .build();
    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
    when(userRepositoryMock.getUserByToken(any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
    when(bridgeApi.findAccountsByToken(JOE_DOE_COGNITO_TOKEN)).thenReturn(List.of());
    Account afterDisconnection = api.getAccountsByUserId(JOE_DOE_ID).get(0);

    assertEquals(beforeDisconnection.getId(), afterDisconnection.getId());
    assertNotNull(beforeDisconnection.getIban());
    assertNotNull(beforeDisconnection.getBank());
    assertNull(afterDisconnection.getIban());
    assertNull(afterDisconnection.getBank());
    assertNull(afterDisconnection.getBic());
  }
  */

  @Test
  void initiate_bank_connection_ok() throws ApiException {
    when(bankRepositoryImplMock.initiateConnection(any()))
        .thenReturn("https://connect.bridgeapi.io");
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);
    String failureUrl = "failure_url";
    String successUrl = "success_url";
    RedirectionStatusUrls redirectionStatusUrls =
        new RedirectionStatusUrls().failureUrl(failureUrl).successUrl(successUrl);

    BankConnectionRedirection actual1 =
        api.initiateBankConnection(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, redirectionStatusUrls);
    BankConnectionRedirection actual2 =
        api.initiateBankConnectionWithoutAccount(JOE_DOE_ID, redirectionStatusUrls);

    assertNotNull(actual1);
    assertTrue(actual1.getRedirectionUrl().contains("https://connect.bridgeapi.io"));
    assertEquals(successUrl, actual1.getRedirectionStatusUrls().getSuccessUrl());
    assertEquals(failureUrl, actual1.getRedirectionStatusUrls().getFailureUrl());

    assertNotNull(actual2);
    assertTrue(actual2.getRedirectionUrl().contains("https://connect.bridgeapi.io"));
    assertEquals(successUrl, actual2.getRedirectionStatusUrls().getSuccessUrl());
    assertEquals(failureUrl, actual2.getRedirectionStatusUrls().getFailureUrl());
  }

  @Test
  void concurrently_get_bridge_accounts() {
    UserAccountsApi api = configureBridgeUserAccountApi(otherBridgeAccount());
    var callerNb = 50;
    var executor = newFixedThreadPool(10);

    var latch = new CountDownLatch(1);
    var futures = new ArrayList<Future<List<Account>>>();
    for (var callerIdx = 0; callerIdx < callerNb; callerIdx++) {
      futures.add(executor.submit(() -> getAccountsByUserId(api, JOE_DOE_ID, latch)));
    }
    latch.countDown();

    List<Account> retrieved =
        futures.stream()
            .map(TestUtils::getOptionalFutureResult)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(Collection::stream)
            // .peek(account -> assertEquals(AccountStatus.OPENED, account.getStatus()))
            .collect(toUnmodifiableList());
    // Since two accounts associated to joe doe user
    assertEquals(callerNb * 2, retrieved.size());
  }

  @Test
  void concurrently_get_bridge_account_holders() {
    UserAccountsApi api = configureBridgeUserAccountApi(otherBridgeAccount());
    var callerNb = 50;
    var executor = newFixedThreadPool(10);

    var latch = new CountDownLatch(1);
    var futures = new ArrayList<Future<List<AccountHolder>>>();
    for (var callerIdx = 0; callerIdx < callerNb; callerIdx++) {
      futures.add(
          executor.submit(() -> getAccountHolders(api, JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, latch)));
    }
    latch.countDown();

    List<AccountHolder> retrieved =
        futures.stream()
            .map(TestUtils::getOptionalFutureResult)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(Collection::stream)
            .peek(
                account ->
                    // TODO: ideally, also test concurrent updates of the balance
                    assertEquals("NUMER", account.getName()))
            .collect(toUnmodifiableList());
    assertEquals(retrieved.size(), callerNb);
  }

  @SneakyThrows
  private static List<Account> getAccountsByUserId(
      UserAccountsApi api, String userId, CountDownLatch latch) {
    latch.await();
    return api.getAccountsByUserId(userId);
  }

  @SneakyThrows
  private static List<AccountHolder> getAccountHolders(
      UserAccountsApi api, String userId, String accountId, CountDownLatch latch) {
    latch.await();
    return api.getAccountHolders(userId, accountId);
  }

  private UserAccountsApi configureBridgeUserAccountApi(BridgeAccount bridgeAccount) {

    reset(userRepositoryMock);
    setUpUserRepositoryWithoutPreferredAccount(userRepositoryMock);
    when(bankRepositoryImplMock.findByExternalId(String.valueOf(joeDoeBridgeAccount().getBankId())))
        .thenReturn(new Bank());
    when(bankRepositoryImplMock.disconnectBank(any())).thenReturn(true);

    ApiClient client = TestUtils.anApiClient(JOE_DOE_COGNITO_TOKEN, null, localPort);
    return new UserAccountsApi(client);
  }

  @Test
  void joe_read_jane_accounts_ko() {
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getAccountsByUserId(JANE_DOE_ID));
  }

  @Test
  void read_other_accounts_ko() {
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getAccountsByUserId(OTHER_USER_ID));
  }

  @Test
  void update_account_identity_ok() throws ApiException {
    setUpBridgeRepositories();
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    Account actual1 = api.updateAccountIdentity(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, bicUpdateOnly());
    Account account1 = filterAccountsById(actual1.getId(), api.getAccountsByUserId(JOE_DOE_ID));
    Account actual2 =
        api.updateAccountIdentity(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, fullUpdateIdentity());
    Account account2 = filterAccountsById(actual1.getId(), api.getAccountsByUserId(JOE_DOE_ID));

    assertEquals(JOE_DOE_ACCOUNT_ID, actual1.getId());
    // actual1 : bic only
    assertEquals(JOE_DOE_ACCOUNT_ID, actual2.getId());
    assertEquals(bicUpdateOnly().getBic(), actual1.getBic());
    assertEquals(bicUpdateOnly().getIban(), actual1.getIban());
    assertEquals(bicUpdateOnly().getName(), actual1.getName());
    assertEquals(
        account1.active(actual1.getActive()), // Not important here
        actual1);
    // actual2 : bic, name, iban
    assertEquals(JOE_DOE_ACCOUNT_ID, actual2.getId());
    assertEquals(fullUpdateIdentity().getBic(), actual2.getBic());
    assertEquals(fullUpdateIdentity().getIban(), actual2.getIban());
    assertEquals(fullUpdateIdentity().getName(), actual2.getName());
    assertEquals(
        account2.active(actual2.getActive()), // Not important here
        actual2);
  }

  private static app.bpartners.api.model.Account joeUpdatedAccount() {
    return joePersistedAccount().toBuilder()
        .name(fullUpdateIdentity().getName())
        .iban(fullUpdateIdentity().getIban())
        .bic(fullUpdateIdentity().getBic())
        .build();
  }

  @Test
  void update_account_identity_ko() {
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"bic is mandatory.\"}",
        () -> api.updateAccountIdentity(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, bicUpdateOnly().bic(null)));
  }
}
