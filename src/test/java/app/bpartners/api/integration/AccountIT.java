package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Account;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.UpdateAccountIdentity;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.swan.BridgeConf;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import app.bpartners.api.repository.bridge.repository.implementation.BridgeAccountRepositoryImpl;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.implementation.BankRepositoryImpl;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.implementation.AccountSwanRepositoryImpl;
import app.bpartners.api.repository.swan.model.SwanAccount;
import app.bpartners.api.repository.swan.response.AccountResponse;
import app.bpartners.api.service.PaymentScheduleService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_CLOSED;
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_CLOSING;
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_OPENED;
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_SUSPENDED;
import static app.bpartners.api.integration.conf.TestUtils.JANE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_COGNITO_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeBridgeAccount;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeSwanAccount;
import static app.bpartners.api.integration.conf.TestUtils.otherBridgeAccount;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AccountIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AccountIT {
  @MockBean
  private PaymentScheduleService paymentScheduleService;
  private static final String OTHER_USER_ID = "OTHER_USER_ID";
  @MockBean
  private BuildingPermitConf buildingPermitConf;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private SwanConf swanConf;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @MockBean
  private AccountHolderSwanRepository accountHolderMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private BridgeBankRepository bridgeBankRepositoryMock;
  @MockBean
  private BankRepositoryImpl bankRepositoryImplMock;
  private AccountSwanRepositoryImpl accountSwanRepositoryImpl;
  private SwanApi swanApiMock;
  private SwanCustomApi swanCustomApiMock;
  @MockBean
  private UserRepository userRepositoryMock;
  @MockBean
  private BridgeApi bridgeApiMock;
  @MockBean
  private BridgeConf bridgeConfMock;
  private BridgeAccountRepositoryImpl bridgeAccountRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  public static AccountResponse.Edge joeDoeEdge() {
    SwanAccount swanAccount2 = joeDoeSwanAccount().toBuilder()
        .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_OPENED))
        .build();
    return AccountResponse.Edge.builder()
        .node(swanAccount2)
        .build();
  }

  public static AccountResponse.Edge openedStatusEdge() {
    SwanAccount swanAccount2 = joeDoeSwanAccount().toBuilder()
        .id(randomUUID().toString())
        .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_OPENED))
        .build();
    return AccountResponse.Edge.builder()
        .node(swanAccount2)
        .build();
  }

  public static AccountResponse.Edge closingStatusEdge() {
    SwanAccount swanAccount3 = joeDoeSwanAccount().toBuilder()
        .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_CLOSING))
        .build();
    return AccountResponse.Edge.builder()
        .node(swanAccount3)
        .build();
  }

  public static AccountResponse.Edge suspendedStatusEdge() {
    SwanAccount swanAccount4 = joeDoeSwanAccount().toBuilder()
        .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_SUSPENDED))
        .build();
    return AccountResponse.Edge.builder()
        .node(swanAccount4)
        .build();
  }

  public static UpdateAccountIdentity accountIdentity() {
    return new UpdateAccountIdentity()
        .name(null)
        .bic("SWNBFR23")
        .iban(null);
  }

  private void setUpUserRepository(UserRepository userRepositoryMock) {
    User user = User.builder()
        .id(JOE_DOE_ID)
        .email("joe@email.com")
        .preferredAccountId(null)
        .account(joeDoeModelAccount())
        .build();
    when(userRepositoryMock.findAll()).thenReturn(List.of(user));
    when(userRepositoryMock.getUserByToken(any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
    when(userRepositoryMock.getUserBySwanUserIdAndToken(any(), any())).thenReturn(user);
  }

  private void setUpUserRepositoryWithPreferredAccount(UserRepository userRepositoryMock) {
    User user = userWithPreferredAccount();
    when(userRepositoryMock.findAll()).thenReturn(List.of(user));
    when(userRepositoryMock.getUserByToken(any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
    when(userRepositoryMock.getUserBySwanUserIdAndToken(any(), any())).thenReturn(user);
  }

  private User userWithoutPreferredAccount() {
    return User.builder()
        .id(JOE_DOE_ID)
        .email("joe@email.com")
        .account(joeDoeModelAccount())
        .preferredAccountId(null)
        .build();
  }

  private User userWithPreferredAccount() {
    return userWithoutPreferredAccount().toBuilder()
        .preferredAccountId(String.valueOf(otherBridgeAccount().getId()))
        .build();
  }

  @BeforeEach
  public void setUp() {
    setUpUserRepository(userRepositoryMock);
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    swanApiMock = mock(SwanApi.class);
    bridgeAccountRepositoryMock = new BridgeAccountRepositoryImpl(bridgeApiMock);
    accountSwanRepositoryImpl = new AccountSwanRepositoryImpl(swanApiMock, swanCustomApiMock);
  }

  Account joeDoeRestAccount() {
    return new Account()
        .id(joeDoeSwanAccount().getId())
        .name(joeDoeSwanAccount().getName())
        .iban(joeDoeSwanAccount().getIban())
        .bic(joeDoeSwanAccount().getBic())
        .iban(joeDoeSwanAccount().getIban())
        .bic(joeDoeSwanAccount().getBic())
        .availableBalance(100000);
  }

  app.bpartners.api.model.Account joeDoeModelAccount() {
    return app.bpartners.api.model.Account.builder()
        .id(joeDoeSwanAccount().getId())
        .name(joeDoeSwanAccount().getName())
        .iban(joeDoeSwanAccount().getIban())
        .bic(joeDoeSwanAccount().getBic())
        .iban(joeDoeSwanAccount().getIban())
        .bic(joeDoeSwanAccount().getBic())
        .bank(Bank.builder().build())
        .availableBalance(parseFraction(100000))
        .build();
  }

  @Test
  void read_opened_accounts_ok() throws ApiException {
    setUpSwanApi(swanApiMock, joeDoeEdge());
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID))
        .thenAnswer(
            invocation ->
                accountSwanRepositoryImpl
                    .findByUserId(invocation.getArgument(0))
        );

    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeRestAccount().status(AccountStatus.OPENED)));
  }

  @Test
  void initiate_bank_connection_ok() throws ApiException {
    when(bridgeBankRepositoryMock.initiateBankConnection("joe@email.com"))
        .thenReturn("https://connect.bridgeapi.io");
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);
    String failureUrl = "failure_url";
    String successUrl = "success_url";
    RedirectionStatusUrls redirectionStatusUrls = new RedirectionStatusUrls()
        .failureUrl(failureUrl)
        .successUrl(successUrl);

    BankConnectionRedirection actual =
        api.initiateBankConnection(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, redirectionStatusUrls);

    assertNotNull(actual);
    assertTrue(actual.getRedirectionUrl().contains("https://connect.bridgeapi.io"));
    assertEquals(successUrl, actual.getRedirectionStatusUrls().getSuccessUrl());
    assertEquals(failureUrl, actual.getRedirectionStatusUrls().getFailureUrl());
  }

  @Test
  void read_closed_accounts_ok() throws ApiException {
    UserAccountsApi api = configureSwanUserAccountsApi(ACCOUNT_CLOSED);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeRestAccount().status(AccountStatus.CLOSED)));
  }

  @Test
  void read_suspended_accounts_ok() throws ApiException {
    UserAccountsApi api = configureSwanUserAccountsApi(ACCOUNT_SUSPENDED);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeRestAccount().status(AccountStatus.SUSPENDED)));
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  public void concurrently_get_bridge_accounts() {
    UserAccountsApi api = configureBridgeUserAccountApi();
    var callerNb = 50;
    var executor = Executors.newFixedThreadPool(10);

    var latch = new CountDownLatch(1);
    var futures = new ArrayList<Future<List<Account>>>();
    for (var callerIdx = 0; callerIdx < callerNb; callerIdx++) {
      futures.add(executor.submit(() -> getAccountsByUserId(api, JOE_DOE_ID, latch)));
    }
    latch.countDown();

    List<Account> retrieved = futures.stream()
        .map(TestUtils::getOptionalFutureResult)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(Collection::stream)
        .peek(account -> assertEquals("Account_name", account.getName()))
        .collect(toUnmodifiableList());
    assertEquals(retrieved.size(), callerNb);
  }

  @Test
  public void concurrently_get_bridge_account_holders() {
    UserAccountsApi api = configureBridgeUserAccountApi();
    var callerNb = 50;
    var executor = Executors.newFixedThreadPool(10);

    var latch = new CountDownLatch(1);
    var futures = new ArrayList<Future<List<AccountHolder>>>();
    for (var callerIdx = 0; callerIdx < callerNb; callerIdx++) {
      futures.add(
          executor.submit(() -> getAccountHolders(api, JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, latch)));
    }
    latch.countDown();

    List<AccountHolder> retrieved = futures.stream()
        .map(TestUtils::getOptionalFutureResult)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(Collection::stream)
        .peek(account ->
            //TODO: ideally, also test concurrent updates of the balance
            assertEquals("NUMER", account.getName()))
        .collect(toUnmodifiableList());
    assertEquals(retrieved.size(), callerNb);
  }

  @SneakyThrows
  private static List<Account> getAccountsByUserId(UserAccountsApi api, String userId,
                                                   CountDownLatch latch) {
    latch.await();
    return api.getAccountsByUserId(userId);
  }

  @SneakyThrows
  private static List<AccountHolder> getAccountHolders(UserAccountsApi api, String userId,
                                                       String accountId, CountDownLatch latch) {
    latch.await();
    return api.getAccountHolders(userId, accountId);
  }

  private UserAccountsApi configureSwanUserAccountsApi(String statusInfo) {
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID)).
        thenReturn(List.of(joeDoeSwanAccount().toBuilder()
            .statusInfo(new SwanAccount.StatusInfo(statusInfo))
            .build()));
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);
    return api;
  }

  private UserAccountsApi configureBridgeUserAccountApi() {
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID)).thenReturn(List.of());
    when(accountSwanRepositoryMock.findByBearer(JOE_DOE_COGNITO_TOKEN)).thenReturn(List.of());

    bridgeAccountRepositoryMock = mock(BridgeAccountRepositoryImpl.class);
    reset(bridgeAccountRepositoryMock);
    when(bridgeAccountRepositoryMock.findAllByAuthenticatedUser()).thenReturn(
        List.of(joeDoeBridgeAccount()));
    when(bridgeAccountRepositoryMock.findByBearer(JOE_DOE_COGNITO_TOKEN)).thenReturn(
        List.of(joeDoeBridgeAccount()));
    when(bankRepositoryImplMock.findByBridgeId(joeDoeBridgeAccount().getBankId())).thenReturn(
        Bank.builder().build());
    ApiClient client = TestUtils.anApiClient(JOE_DOE_COGNITO_TOKEN, ContextInitializer.SERVER_PORT);
    return new UserAccountsApi(client);
  }

  private void setPrincipalProvider() {
    Principal principal = mock(Principal.class);
    doReturn(User.builder()
        .id(JOE_DOE_ID)
        .email("joe@email.com")
        .account(joeDoeModelAccount())
        .preferredAccountId(null)
        .build()).when(principal).getUser();
    doReturn(JOE_DOE_TOKEN).when(principal).getBearer();
    MockedStatic<AuthProvider> authProviderMock = Mockito.mockStatic(AuthProvider.class);
    authProviderMock.when(AuthProvider::getPrincipal)
        .thenReturn(principal);
  }

  @Test
  void read_closing_accounts_ok() throws ApiException {
    UserAccountsApi api = configureSwanUserAccountsApi(ACCOUNT_CLOSING);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeRestAccount().status(AccountStatus.CLOSING)));
  }

  @Test
  void read_unknown_accounts_ok() throws ApiException {
    UserAccountsApi api = configureSwanUserAccountsApi("Unknown status");

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeRestAccount().status(AccountStatus.UNKNOWN)));
  }

  @Test
  void joe_read_jane_accounts_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getAccountsByUserId(JANE_DOE_ID));
  }

  @Test
  void read_from_multiple_accounts_ok() throws ApiException {
    setUpSwanApi(swanApiMock, joeDoeEdge(), closingStatusEdge(), suspendedStatusEdge());
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID))
        .thenAnswer(
            invocation ->
                accountSwanRepositoryImpl
                    .findByUserId(invocation.getArgument(0))
        );

    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeRestAccount().status(AccountStatus.OPENED)));
  }

  void setUpBridge(BridgeApi bridgeApi, BridgeAccount... accounts) {
    when(bridgeApi.findAccountsByToken(JOE_DOE_COGNITO_TOKEN))
        .thenReturn(new ArrayList<>() {
          {
            this.addAll(List.of(accounts));
          }
        });
  }

  void setUpSwanApi(SwanApi swanApi, AccountResponse.Edge... edges) {
    when(swanApi.getData(eq(AccountResponse.class), any(String.class)))
        .thenReturn(
            AccountResponse.builder()
                .data(
                    AccountResponse.Data.builder()
                        .accounts(
                            AccountResponse.Accounts.builder()
                                .edges(
                                    new ArrayList<>() {
                                      {
                                        this.addAll(List.of(edges));
                                      }
                                    }
                                ).build()
                        ).build()
                ).build()
        );
  }

  @Test
  void read_from_multiple_accounts_ko() throws ApiException {
    setUpSwanApi(
        swanApiMock,
        openedStatusEdge(),
        openedStatusEdge(),
        closingStatusEdge(),
        suspendedStatusEdge()
    );
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID))
        .thenAnswer(
            invocation ->
                accountSwanRepositoryImpl
                    .findByUserId(invocation.getArgument(0))
        );

    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\","
            + "\"message\":\"One user with one active account is supported for now\"}",
        () -> api.getAccountsByUserId(JOE_DOE_ID)
    );
  }

  @Test
  void read_empty_accounts_ko() {
    setUpSwanApi(swanApiMock);
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID))
        .thenAnswer(
            invocation ->
                accountSwanRepositoryImpl
                    .findByUserId(invocation.getArgument(0))
        );

    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\","
            + "\"message\":\"One user should have at least one account\"}",
        () -> api.getAccountsByUserId(JOE_DOE_ID)
    );
  }

  @Test
  void read_without_active_account_ko() {
    setUpSwanApi(swanApiMock, closingStatusEdge());
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID))
        .thenAnswer(
            invocation ->
                accountSwanRepositoryImpl
                    .findByUserId(invocation.getArgument(0))
        );

    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\","
            + "\"message\":\"One user should have one active account"
            + " but following not active are present :"
            + " account.beed1765-5c16-472a-b3f4-5c376ce5db58\"}",
        () -> api.getAccountsByUserId(JOE_DOE_ID)
    );
  }

  @Test
  void read_other_accounts_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getAccountsByUserId(OTHER_USER_ID));
  }

  @Test
  void update_account_identity_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    Account actual = api.updateAccountIdentity(
        JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, accountIdentity());

    assertEquals(joeDoeRestAccount().getId(), actual.getId());
    assertEquals(accountIdentity().getBic(), actual.getBic());
    assertEquals(joeDoeRestAccount().getIban(), actual.getIban());
    assertEquals(joeDoeRestAccount().getName(), actual.getName());
  }

  @Test
  void update_account_identity_ko() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"bic is mandatory.\"}"
        , () -> api.updateAccountIdentity(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID,
            accountIdentity().bic(null)));
  }

  @Test
  void read_preferred_bridge_account() throws ApiException {
    //Do not touch
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID)).thenReturn(List.of());
    when(accountSwanRepositoryMock.findByBearer(JOE_DOE_COGNITO_TOKEN)).thenReturn(List.of());
    //END
    reset(userRepositoryMock);
    setUpUserRepositoryWithPreferredAccount(userRepositoryMock);
    setUpBridge(bridgeApiMock, joeDoeBridgeAccount(), otherBridgeAccount());
    when(bankRepositoryImplMock.findByBridgeId(joeDoeBridgeAccount().getBankId())).thenReturn(
        Bank.builder().build());
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertEquals(1, actual.size());
    assertEquals(otherBridgeAccount().getName(), actual.get(0).getName());
    assertEquals(otherBridgeAccount().getIban(), actual.get(0).getIban());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
