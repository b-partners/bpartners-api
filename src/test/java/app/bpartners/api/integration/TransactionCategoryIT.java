package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
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

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.restTransaction2;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpTransactionRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TransactionCategoryIT.ContextInitializer.class)
@AutoConfigureMockMvc
class TransactionCategoryIT {
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

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpTransactionRepository(transactionSwanRepositoryMock);
  }

  CreateTransactionCategory createTransactionCategory() {
    return new CreateTransactionCategory()
        .type("Recette TVA 1,5%")
        .vat(150.0);
  }

  @Test
  void read_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actualAll = api.getTransactionCategories(JOE_DOE_ACCOUNT_ID,
        LocalDate.now(), LocalDate.now(), null);
    List<TransactionCategory> actualUserNotDefined =
        api.getTransactionCategories(JOE_DOE_ACCOUNT_ID, LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 1, 2), false);
    List<TransactionCategory> actualUserDefined =
        api.getTransactionCategories(JOE_DOE_ACCOUNT_ID, LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 1, 2), true);

    assertEquals(33, actualAll.size());
    assertEquals(31, actualUserNotDefined.size());
    assertEquals(2, actualUserDefined.size());
  }

  @Test
  void count_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actualAll = api.getTransactionCategories(JOE_DOE_ACCOUNT_ID,
        LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), null);

    assertEquals(33, actualAll.size());
    assertTrue(actualAll.stream().noneMatch(e -> e.getCount() != 0L));
  }

  @Test
  void create_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actual = api.createTransactionCategories(JOE_DOE_ACCOUNT_ID,
        restTransaction2().getId(),
        List.of(createTransactionCategory()));

    assertEquals(1, actual.size());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
