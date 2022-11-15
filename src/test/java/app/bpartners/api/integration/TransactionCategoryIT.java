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

import static app.bpartners.api.endpoint.rest.model.TransactionTypeEnum.INCOME;
import static app.bpartners.api.endpoint.rest.model.TransactionTypeEnum.OUTCOME;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
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
  private static final String UNKNOWN_CATEGORY_TYPE = "unknown_type";
  private static final String UNKNOWN_TRANSACTION_ID = "unknown_id";
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

  CreateTransactionCategory incomeCategory() {
    return new CreateTransactionCategory()
        .type("Recette TVA 20%");
  }

  CreateTransactionCategory outcomeCategory() {
    return new CreateTransactionCategory()
        .type("Achat TVA 20%");
  }

  CreateTransactionCategory otherOutcomeCategory() {
    return new CreateTransactionCategory()
        .type("Autres dépenses");
  }

  @Test
  void read_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actualAll = api.getTransactionCategories(JOE_DOE_ACCOUNT_ID,
        LocalDate.now(), LocalDate.now(), null);
    /*
    TODO:
    Expected should be at least 12 because all the categories template should be sent
    Others categories templates should also be returned with their comment values
     */
    assertEquals(8, actualAll.size());
  }

  @Test
  void count_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    LocalDate startOf2021 = LocalDate.of(2021, 1, 1);
    LocalDate endOf2021 = LocalDate.of(2021, 12, 31);
    LocalDate startOf2022 = LocalDate.of(2022, 1, 1);
    LocalDate endOf2022 = LocalDate.of(2022, 12, 31);

    List<TransactionCategory> actualYear2021 =
        api.getTransactionCategories(JOE_DOE_ACCOUNT_ID, startOf2021, endOf2021, null);
    List<TransactionCategory> actualYear2022 =
        api.getTransactionCategories(JOE_DOE_ACCOUNT_ID, startOf2022, endOf2022, null);

    assertTrue(actualYear2021.stream()
        .allMatch(category -> category.getCount() == 0L));
    assertTrue(actualYear2022.stream()
        .noneMatch(category -> category.getCount() == 0L));
  }

  @Test
  void create_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actual = api.createTransactionCategories(JOE_DOE_ACCOUNT_ID,
        restTransaction2().getId(),
        List.of(incomeCategory()));

    assertEquals(1, actual.size());
  }

  @Test
  void create_transaction_categories_ko() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\","
            + "\"message\":\"Transaction category " + UNKNOWN_CATEGORY_TYPE + " not found."
            + " Creation of a new one is not supported yet.\""
            + "}",
        () -> api.createTransactionCategories(JOE_DOE_ACCOUNT_ID
            , restTransaction2().getId(),
            List.of(new CreateTransactionCategory().type(UNKNOWN_CATEGORY_TYPE)))
    );
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            +
            "\"message\":\"Transaction category of type " + OUTCOME
            + " cannot be added to transaction of type " + INCOME
            + "\"}",
        () -> api.createTransactionCategories(JOE_DOE_ACCOUNT_ID,
            restTransaction2().getId(),
            List.of(outcomeCategory())
        ));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            +
            "\"message\":\"Transaction category of type " + OUTCOME
            + " cannot be added to transaction of type " + INCOME
            + "\"}",
        () -> api.createTransactionCategories(JOE_DOE_ACCOUNT_ID,
            restTransaction2().getId(),
            List.of(otherOutcomeCategory()))
    );
    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\","
            +
            "\"message\":\"Transaction." + UNKNOWN_TRANSACTION_ID + " not found.\"}",
        () -> api.createTransactionCategories(JOE_DOE_ACCOUNT_ID, UNKNOWN_TRANSACTION_ID,
            List.of(incomeCategory()))
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
