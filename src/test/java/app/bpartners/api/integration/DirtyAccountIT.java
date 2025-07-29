package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@Disabled("TODO(fail)")
class DirtyAccountIT extends MockedThirdParties {
  @MockBean private UserRepository userRepositoryMock;
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

  @Test
  void update_account_identity_ko() {
    ApiClient joeDoeClient = joeDoeClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"bic is mandatory.\"}",
        () -> api.updateAccountIdentity(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID, bicUpdateOnly().bic(null)));
  }
}
