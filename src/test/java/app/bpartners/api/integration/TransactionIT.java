package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.MonthlyTransactionsSummary;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionsSummary;
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
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JANE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.restTransaction1;
import static app.bpartners.api.integration.conf.TestUtils.restTransaction2;
import static app.bpartners.api.integration.conf.TestUtils.restTransaction3;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpTransactionRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static java.util.Calendar.DECEMBER;
import static java.util.Calendar.JANUARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TransactionIT.ContextInitializer.class)
@AutoConfigureMockMvc
class TransactionIT {
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
  private TransactionSwanRepository transactionSwanRepositoryMock;
  @MockBean
  private AccountHolderSwanRepository accountHolderMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        TransactionIT.ContextInitializer.SERVER_PORT);
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  MonthlyTransactionsSummary month1() {
    return new MonthlyTransactionsSummary()
        .id("monthly_transactions_summary1_id")
        .month(JANUARY)
        .income(1000000)
        .outcome(0)
        .cashFlow(1000000);
  }

  MonthlyTransactionsSummary month2() {
    return new MonthlyTransactionsSummary()
        .id("monthly_transactions_summary2_id")
        .month(DECEMBER)
        .income(0)
        .outcome(0)
        .cashFlow(1000000);
  }

  TransactionsSummary transactionsSummary1() {
    return new TransactionsSummary()
        .year(2022)
        .summary(List.of(month1(), month2()));
  }

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpTransactionRepository(transactionSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  @Test
  void read_transactions_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actual = api.getTransactions(JOE_DOE_ACCOUNT_ID, null, null);
    assertEquals(3, actual.size());
    assertTrue(actual.contains(restTransaction1()));
    assertTrue(actual.contains(restTransaction2()));
    assertTrue(actual.contains(restTransaction3()));
  }

  @Test
  void john_read_transactions_summary_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    int currentYear = LocalDate.now().getYear();

    TransactionsSummary actualDefaultYear = api.getTransactionsSummary(JOE_DOE_ACCOUNT_ID, null);
    TransactionsSummary actualCustomYear = api.getTransactionsSummary(JOE_DOE_ACCOUNT_ID,
        currentYear + 1);

    assertEquals(2, actualDefaultYear.getSummary().size());
    assertEquals(0, actualCustomYear.getSummary().size());
    assertEquals(currentYear + 1, actualCustomYear.getYear());
    assertEquals(transactionsSummary1(),
        actualDefaultYear.summary(ignoreUpdatedAt(actualDefaultYear.getSummary())));
  }

  @Test
  void jane_read_transactions_summary_ok() throws ApiException {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    PayingApi api = new PayingApi(janeDoeClient);
    int currentYear = LocalDate.now().getYear();

    TransactionsSummary actualDefaultYear = api.getTransactionsSummary(JANE_ACCOUNT_ID, null);
    TransactionsSummary actualCustomYear =
        api.getTransactionsSummary(JANE_ACCOUNT_ID, currentYear + 1);

    assertEquals(0, actualDefaultYear.getSummary().size());
    assertEquals(0, actualCustomYear.getSummary().size());
  }

  List<MonthlyTransactionsSummary> ignoreUpdatedAt(List<MonthlyTransactionsSummary> actual) {
    actual.forEach(monthlyTransactionsSummary -> {
      monthlyTransactionsSummary.setUpdatedAt(null);
    });
    return actual;
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
