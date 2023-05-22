package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Account;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.model.AccountValidationRedirection;
import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.UpdateAccountIdentity;
import app.bpartners.api.endpoint.rest.security.swan.BridgeConf;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.model.Item.BridgeConnectItem;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.implementation.BankRepositoryImpl;
import app.bpartners.api.repository.implementation.SavableAccountConnectorRepository;
import app.bpartners.api.repository.model.AccountConnector;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.implementation.SwanAccountConnectorRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
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
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_UNKNOWN_STATUS;
import static app.bpartners.api.integration.conf.TestUtils.BERNARD_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.BERNARD_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.BERNARD_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.JANE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_COGNITO_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_FAILURE_URL;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_SUCCESS_URL;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.bernardDoeSwanAccount;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeBridgeAccount;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeSwanAccount;
import static app.bpartners.api.integration.conf.TestUtils.otherBridgeAccount;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountConnectorSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpBernardUserSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.toConnector;
import static app.bpartners.api.repository.bridge.model.Account.BridgeAccount.BRIDGE_STATUS_OK;
import static app.bpartners.api.repository.bridge.model.Account.BridgeAccount.BRIDGE_STATUS_SCA;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.AFTER_METHOD;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AccountIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AccountIT {
  private static final String OTHER_USER_ID = "OTHER_USER_ID";
  @MockBean
  private PaymentScheduleService paymentScheduleService;
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
  private SwanCustomApi swanCustomApi;
  @MockBean
  private BridgeConf bridgeConf;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private SwanAccountConnectorRepository swanAccountConnectorRepositoryMock;
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
  @MockBean
  private SwanApi swanApiMock;
  @Autowired
  private SavableAccountConnectorRepository savableAccountConnectorRepository;
  @Autowired
  private AccountMapper accountMapper;
  @MockBean
  private UserRepository userRepositoryMock;
  @MockBean
  private UserTokenRepository userTokenRepositoryMock;
  @MockBean
  private BridgeApi bridgeApiMock;
  private AccountConnectorRepository accountConnectorRepositoryMock;

  private static ApiClient joeDoeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  private static ApiClient bernardDoeClient() {
    return TestUtils.anApiClient(BERNARD_DOE_TOKEN, ContextInitializer.SERVER_PORT);
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

  AccountValidationRedirection accountValidationRedirection() {
    return new AccountValidationRedirection()
        .redirectionUrl("https://connect.bridge.io")
        .redirectionStatusUrls(new RedirectionStatusUrls()
            .successUrl(REDIRECT_SUCCESS_URL)
            .failureUrl(REDIRECT_FAILURE_URL));
  }

  public static UpdateAccountIdentity accountIdentity() {
    return new UpdateAccountIdentity()
        .name(null)
        .bic("SWNBFR23")
        .iban(null);
  }

  User joeDoeUser() {
    return User.builder()
        .id(JOE_DOE_ID)
        .email("joe@email.com")
        .preferredAccountId(null)
        .accounts(List.of(joeDoeModelAccount()))
        .build();
  }

  User bernardUser() {
    return User.builder()
        .id(BERNARD_DOE_ID)
        .email("bernard@email.com")
        .preferredAccountId(null)
        .accounts(List.of(bernardDoeModelAccount()))
        .build();
  }

  private User userWithPreferredAccount() {
    return User.builder()
        .id(JOE_DOE_ID)
        .preferredAccountId(String.valueOf(otherBridgeAccount().getId()))
        .email("joe@email.com")
        .accounts(List.of(joeDoeModelAccount()))
        .build();
  }

  Account joeDoeRestAccount() {
    return new Account()
        .id(JOE_DOE_ACCOUNT_ID)
        .name(joeDoeSwanAccount().getName())
        .iban(joeDoeSwanAccount().getIban())
        .bic(joeDoeSwanAccount().getBic())
        .iban(joeDoeSwanAccount().getIban())
        .bic("BIC_NOT_NULL")
        .active(true)
        .availableBalance(100000);
  }

  app.bpartners.api.model.Account joeDoeModelAccount() {
    return app.bpartners.api.model.Account.builder()
        .id(joeDoeSwanAccount().getId())
        .userId(JOE_DOE_ID)
        .name(joeDoeSwanAccount().getName())
        .iban(joeDoeSwanAccount().getIban())
        .bic(joeDoeSwanAccount().getBic())
        .status(AccountStatus.OPENED)
        .bank(Bank.builder().build())
        .availableBalance(parseFraction(100000))
        .active(true)
        .build();
  }

  app.bpartners.api.model.Account bernardDoeModelAccount() {
    return app.bpartners.api.model.Account.builder()
        .id(bernardDoeSwanAccount().getId())
        .name(bernardDoeSwanAccount().getName())
        .iban(bernardDoeSwanAccount().getIban())
        .bic(bernardDoeSwanAccount().getBic())
        .status(AccountStatus.VALIDATION_REQUIRED)
        .bank(Bank.builder().build())
        .availableBalance(parseFraction(100000))
        .active(true)
        .build();
  }

  private void setUpUserRepository(UserRepository userRepositoryMock) {
    when(userRepositoryMock.findAll()).thenReturn(List.of(joeDoeUser()));
    when(userRepositoryMock.getUserByToken(any())).thenReturn(joeDoeUser());
    when(userRepositoryMock.getByEmail(any())).thenReturn(joeDoeUser());
    when(userRepositoryMock.getById(any())).thenReturn(joeDoeUser());
    when(userRepositoryMock.getUserBySwanUserIdAndToken(any(), any())).thenReturn(joeDoeUser());
  }

  private void setUpUserBernardRepository(UserRepository userRepositoryMock) {
    when(userRepositoryMock.findAll()).thenReturn(List.of(bernardUser()));
    when(userRepositoryMock.getUserByToken(any())).thenReturn(bernardUser());
    when(userRepositoryMock.getByEmail(any())).thenReturn(bernardUser());
    when(userRepositoryMock.getUserBySwanUserIdAndToken(any(), any())).thenReturn(bernardUser());
  }

  private void setUpUserRepositoryWithPreferredAccount(UserRepository userRepositoryMock) {
    User user = userWithPreferredAccount();


    when(userRepositoryMock.findAll()).thenReturn(List.of(user));
    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(userRepositoryMock.getUserByToken(any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
    when(userRepositoryMock.getUserBySwanUserIdAndToken(any(), any())).thenReturn(user);
  }

  private void setUpUserRepositoryWithoutPreferredAccount(UserRepository userRepositoryMock) {
    User user = userWithPreferredAccount().toBuilder()
        .preferredAccountId(null)
        .build();
    when(userRepositoryMock.findAll()).thenReturn(List.of(user));
    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(userRepositoryMock.getUserByToken(any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
    when(userRepositoryMock.getUserBySwanUserIdAndToken(any(), any())).thenReturn(user);
  }

  private void setUpBridgeRepositories() {
    when(swanAccountConnectorRepositoryMock.findByUserId(JOE_DOE_ID)).thenReturn(List.of());
    when(swanAccountConnectorRepositoryMock.findByBearer(JOE_DOE_COGNITO_TOKEN)).thenReturn(
        List.of());

    reset(userRepositoryMock);
    setUpUserRepositoryWithPreferredAccount(userRepositoryMock);
    setUpBridge(bridgeApiMock, joeDoeBridgeAccount(), otherBridgeAccount());
    when(bankRepositoryImplMock.findByExternalId(
        String.valueOf(joeDoeBridgeAccount().getBankId()))).thenReturn(new Bank());
    when(bankRepositoryImplMock.disconnectBank(any())).thenReturn(true);
  }

  @BeforeEach
  public void setUp() {
    setUpUserRepository(userRepositoryMock);
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountConnectorSwanRepository(swanAccountConnectorRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    swanApiMock = mock(SwanApi.class);
    accountConnectorRepositoryMock =
        new SwanAccountConnectorRepository(
            savableAccountConnectorRepository, swanApiMock, swanCustomApi, accountMapper);
  }

  /*
  @Test
  void disconnect_bank_ok() throws ApiException {
    setUpBridgeRepositories();
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);
    Account beforeDisconnection = api.getAccountsByUserId(JOE_DOE_ID).get(0);

    api.disconnectBank(JOE_DOE_ID);
    reset(bridgeApiMock);
    reset(userRepositoryMock);
    User user = User.builder()
        .id(JOE_DOE_ID)
        .email("joe@email.com")
        .accounts(List.of(joeDoeModelAccount()))
        .build();
    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
    when(userRepositoryMock.getUserByToken(any())).thenReturn(user);
    when(userRepositoryMock.getUserBySwanUserIdAndToken(any(), any())).thenReturn(user);
    when(userRepositoryMock.getByEmail(any())).thenReturn(user);
    when(bridgeApiMock.findAccountsByToken(JOE_DOE_COGNITO_TOKEN)).thenReturn(List.of());
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
  void read_opened_accounts_ok() throws ApiException {
    UserAccountsApi api = configureSwanUserAccountsApi(ACCOUNT_OPENED);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeRestAccount().status(AccountStatus.OPENED)));
  }

  @Test
  @DirtiesContext(methodMode = AFTER_METHOD)
  void initiate_bank_connection_ok() throws ApiException {
    when(bankRepositoryImplMock.initiateConnection(any()))
        .thenReturn("https://connect.bridgeapi.io");
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);
    String failureUrl = "failure_url";
    String successUrl = "success_url";
    RedirectionStatusUrls redirectionStatusUrls = new RedirectionStatusUrls()
        .failureUrl(failureUrl)
        .successUrl(successUrl);

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
    UserAccountsApi api = configureBridgeUserAccountApi(otherBridgeAccount());
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
        //.peek(account -> assertEquals(AccountStatus.OPENED, account.getStatus()))
        .collect(toUnmodifiableList());
    // Since two accounts associated to joe doe user
    assertEquals(callerNb * 2, retrieved.size());
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  public void concurrently_get_bridge_account_holders() {
    UserAccountsApi api = configureBridgeUserAccountApi(otherBridgeAccount());
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
  private static List<AccountHolder> getAccountHolders(
      UserAccountsApi api, String userId, String accountId, CountDownLatch latch) {
    latch.await();
    return api.getAccountHolders(userId, accountId);
  }

  private UserAccountsApi configureSwanUserAccountsApi(String statusInfo) {
    AccountConnector accountConnector = toConnector(joeDoeSwanAccount().toBuilder()
        .statusInfo(new SwanAccount.StatusInfo(statusInfo))
        .build());
    when(swanAccountConnectorRepositoryMock.findByUserId(JOE_DOE_ID)).
        thenReturn(List.of(accountConnector));
    when(swanAccountConnectorRepositoryMock.saveAll(JOE_DOE_ID, List.of(accountConnector)))
        .thenReturn(List.of(accountConnector));
    when(swanAccountConnectorRepositoryMock.save(JOE_DOE_ID, accountConnector)).thenReturn(
        accountConnector);
    ApiClient joeDoeClient = joeDoeClient();
    return new UserAccountsApi(joeDoeClient);
  }

  private UserAccountsApi configureBridgeUserAccountApi(BridgeAccount bridgeAccount) {
    when(swanAccountConnectorRepositoryMock.findByUserId(JOE_DOE_ID)).thenReturn(List.of());
    when(swanAccountConnectorRepositoryMock.findByBearer(JOE_DOE_COGNITO_TOKEN)).thenReturn(
        List.of());

    reset(userRepositoryMock);
    setUpUserRepositoryWithoutPreferredAccount(userRepositoryMock);
    setUpBridge(bridgeApiMock, bridgeAccount);
    when(bankRepositoryImplMock.findByExternalId(
        String.valueOf(joeDoeBridgeAccount().getBankId()))).thenReturn(new Bank());
    when(bankRepositoryImplMock.disconnectBank(any())).thenReturn(true);
    ApiClient client = TestUtils.anApiClient(JOE_DOE_COGNITO_TOKEN, ContextInitializer.SERVER_PORT);
    return new UserAccountsApi(client);
  }

  @Test
  void read_closing_accounts_ok() throws ApiException {
    UserAccountsApi api = configureSwanUserAccountsApi(ACCOUNT_CLOSING);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeRestAccount().status(AccountStatus.CLOSING)));
  }

  @Test
  @DirtiesContext(methodMode = BEFORE_METHOD)
  void read_unknown_accounts_ok() throws ApiException {
    UserAccountsApi api = configureSwanUserAccountsApi(ACCOUNT_UNKNOWN_STATUS);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeRestAccount().status(AccountStatus.UNKNOWN)));
  }

  @Test
  void joe_read_jane_accounts_ko() {
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getAccountsByUserId(JANE_DOE_ID));
  }

  //TODO
  //  @Test
  //  void read_from_multiple_swan_accounts_ok() {
  //  }

  //TODO
  //  @Test
  // void read_from_multiple_swan_accounts_ko() {
  // }

  //TODO
  //  @Test
  //  void read_without_opened_swan_account_ko() {
  //    assertTrue(true);
  //  }

  @Test
  void read_other_accounts_ko() {
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getAccountsByUserId(OTHER_USER_ID));
  }

  @Test
  @DirtiesContext(methodMode = AFTER_METHOD)
  void update_account_identity_ok() throws ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);
    String persistedName = "Other";
    String persistedIban = "FR12349001001190346460988";

    Account actual = api.updateAccountIdentity(
        JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, accountIdentity());

    assertEquals(joeDoeRestAccount().getId(), actual.getId());
    assertEquals(accountIdentity().getBic(), actual.getBic());
    assertEquals(persistedIban, actual.getIban());
    assertEquals(persistedName, actual.getName());
  }

  @Test
  void update_account_identity_ko() {
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"bic is mandatory.\"}"
        , () -> api.updateAccountIdentity(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID,
            accountIdentity().bic(null)));
  }

  //TODO: check when external ID is not associated to account
  @Test
  void read_preferred_bridge_account() throws ApiException {
    setUpBridgeRepositories();
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertEquals(3, actual.size());
    Account activeAccount = actual.stream()
        .filter(Account::getActive)
        .findAny()
        .get();
    assertEquals(otherBridgeAccount().getName(), activeAccount.getName());
    assertEquals(otherBridgeAccount().getIban(), activeAccount.getIban());

  }

  @Test
  @DirtiesContext(methodMode = AFTER_METHOD)
  void validate_bank_connection_ok() throws ApiException {
    final String redirectUrl = "https://connect.bridge.io";
    ApiClient bernarDoeClient = bernardDoeClient();
    UserAccountsApi api = new UserAccountsApi(bernarDoeClient);

    reset(userRepositoryMock);
    reset(userSwanRepositoryMock);
    setUpUserBernardRepository(userRepositoryMock);
    setUpBernardUserSwanRepository(userSwanRepositoryMock);
    when(bridgeBankRepositoryMock.validateCurrentProItems(BERNARD_DOE_TOKEN))
        .thenReturn(BridgeConnectItem.builder()
            .redirectUrl(redirectUrl)
            .build()
        );
    when(bankRepositoryImplMock.initiateProValidation(any()))
        .thenReturn(redirectUrl);
    when(userTokenRepositoryMock.getLatestTokenByAccount(any()))
        .thenReturn(UserToken.builder()
            .accessToken(BERNARD_DOE_TOKEN)
            .user(bernardUser())
            .build());
    when(swanAccountConnectorRepositoryMock.save(any(), any())).thenReturn(
        toConnector(bernardDoeSwanAccount()));
    when(swanAccountConnectorRepositoryMock.saveAll(any(), any())).thenReturn(
        List.of(toConnector(bernardDoeSwanAccount())));

    AccountValidationRedirection actual = api.initiateAccountValidation(BERNARD_DOE_ID,
        BERNARD_DOE_ACCOUNT_ID, accountValidationRedirection().getRedirectionStatusUrls());

    assertEquals(accountValidationRedirection(), actual);
  }

  @Test
  void manage_bank_connection_with_strong_auth_ok() throws ApiException {
    final String redirectUrl = "https://connect.bridge.io";
    when(bankRepositoryImplMock.initiateScaSync(any()))
        .thenReturn(redirectUrl);
    BridgeAccount scaRequiredAccount = otherBridgeAccount().toBuilder()
        .status(BRIDGE_STATUS_SCA)
        .build();
    UserAccountsApi api = configureBridgeUserAccountApi(scaRequiredAccount);

    AccountValidationRedirection actual =
        api.initiateAccountValidation(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, new RedirectionStatusUrls());

    assertTrue(actual.getRedirectionUrl().contains(redirectUrl));
  }

  @Test
  void manage_bank_connection_with_strong_auth_ko() {
    final String redirectUrl = "https://connect.bridge.io";
    when(bankRepositoryImplMock.initiateScaSync(any()))
        .thenReturn(redirectUrl);
    BridgeAccount scaRequiredAccount = otherBridgeAccount().toBuilder()
        .status(BRIDGE_STATUS_OK)
        .build();
    UserAccountsApi api = configureBridgeUserAccountApi(scaRequiredAccount);

    assertThrowsApiException("{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"Account("
            + "id=beed1765-5c16-472a-b3f4-5c376ce5db58,"
            + "name=Other,"
            + "iban=FR12349001001190346460988,status=OPENED,active=false)"
            + " does not need validation.\"}",
        () -> api.initiateAccountValidation(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID,
            new RedirectionStatusUrls()));
  }

  @Test
  void validate_bank_connection_ko() {
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    when(userTokenRepositoryMock.getLatestTokenByAccount(any()))
        .thenReturn(UserToken.builder()
            .accessToken(JOE_DOE_TOKEN)
            .user(joeDoeUser())
            .build());

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\""
            + "message\":\"Account(id=beed1765-5c16-472a-b3f4-5c376ce5db58,name=Account_name,"
            + "iban=FR0123456789,status=OPENED,active=false) does not need validation.\"}",
        () -> api.initiateAccountValidation(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID,
            new RedirectionStatusUrls()));
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

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
