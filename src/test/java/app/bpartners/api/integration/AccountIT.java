package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Account;
import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.model.SwanAccount;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_CLOSED;
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_CLOSING;
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_OPENED;
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_SUSPENDED;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeSwanAccount;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AccountIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AccountIT {
  private static final String OTHER_USER_ID = "OTHER_USER_ID";
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

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  Account joeDoeAccount() {
    return new Account()
        .id(joeDoeSwanAccount().getId())
        .name(joeDoeSwanAccount().getName())
        .iban(joeDoeSwanAccount().getIban())
        .bic(joeDoeSwanAccount().getBic())
        .IBAN(joeDoeSwanAccount().getIban())
        .BIC(joeDoeSwanAccount().getBic())
        .availableBalance(100000);
  }

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  @Test
  void read_opened_accounts_ok() throws ApiException {
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID)).
        thenReturn(List.of(joeDoeSwanAccount().toBuilder()
            .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_OPENED))
            .build()));
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeAccount().status(AccountStatus.OPENED)));
  }

  @Test
  void read_closed_accounts_ok() throws ApiException {
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID)).
        thenReturn(List.of(joeDoeSwanAccount().toBuilder()
            .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_CLOSED))
            .build()));
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeAccount().status(AccountStatus.CLOSED)));
  }

  @Test
  void read_suspended_accounts_ok() throws ApiException {
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID)).
        thenReturn(List.of(joeDoeSwanAccount().toBuilder()
            .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_SUSPENDED))
            .build()));
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeAccount().status(AccountStatus.SUSPENDED)));
  }

  @Test
  void read_closing_accounts_ok() throws ApiException {
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID)).
        thenReturn(List.of(joeDoeSwanAccount().toBuilder()
            .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_CLOSING))
            .build()));
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeAccount().status(AccountStatus.CLOSING)));
  }

  @Test
  void read_unknown_accounts_ok() throws ApiException {
    when(accountSwanRepositoryMock.findByUserId(JOE_DOE_ID)).
        thenReturn(List.of(joeDoeSwanAccount().toBuilder()
            .statusInfo(new SwanAccount.StatusInfo("Unknown status"))
            .build()));
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<Account> actual = api.getAccountsByUserId(JOE_DOE_ID);

    assertTrue(actual.contains(joeDoeAccount().status(AccountStatus.UNKNOWN)));
  }

  @Test
  void read_accounts_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getAccountsByUserId(OTHER_USER_ID));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
