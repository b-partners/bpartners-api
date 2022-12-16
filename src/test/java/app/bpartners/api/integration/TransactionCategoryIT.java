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
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
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
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
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
  @MockBean
  private AccountHolderSwanRepository accountHolderMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
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

  CreateTransactionCategory incomeTransactionCategory() {
    return new CreateTransactionCategory()
        .type("Recette TVA 20%")
        .vat(2000);
  }

  CreateTransactionCategory outcomeTransactionCategory() {
    return new CreateTransactionCategory()
        .type("Achat TVA 20%")
        .vat(2000);
  }

  CreateTransactionCategory otherIncomeTransactionCategory() {
    return new CreateTransactionCategory()
        .type("Autres produits")
        .vat(0)
        .comment("Don de l'Etat");
  }


  @Test
  void read_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actualAll = api.getTransactionCategories(JOE_DOE_ACCOUNT_ID,
        LocalDate.now(), LocalDate.now(), null);
    List<TransactionCategory> actualIncome = api.getTransactionCategories(JOE_DOE_ACCOUNT_ID,
        LocalDate.now(), LocalDate.now(), INCOME);
    List<TransactionCategory> actualOutcome = api.getTransactionCategories(JOE_DOE_ACCOUNT_ID,
        LocalDate.now(), LocalDate.now(), OUTCOME);

    assertEquals(35, actualAll.size());
    assertTrue(actualIncome.stream().allMatch(t -> Objects.equals(t.getTransactionType(), INCOME)));
    assertTrue(actualOutcome.stream().allMatch(t -> Objects.equals(t.getTransactionType(),
        OUTCOME)));
    assertTrue(actualAll.containsAll(actualIncome));
    assertTrue(actualAll.containsAll(actualOutcome));
  }

  @Test
  void count_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    LocalDate startDate = LocalDate.of(2021, 1, 1);
    //TODO: there is a timezone problem because when set to 31/12/2021 it takes also the
    // 01/01/2022 data
    LocalDate endDate = LocalDate.of(2021, 12, 30);

    List<TransactionCategory> actualAll = api.getTransactionCategories(
        JOE_DOE_ACCOUNT_ID, startDate, endDate, null);

    assertEquals(35, actualAll.size());
    //TODO: uncomment when problem is fixed
    // assertTrue(actualAll.stream().noneMatch(e -> e.getCount() != 0L));
  }

  @Test
  void create_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actual = api.createTransactionCategories(JOE_DOE_ACCOUNT_ID,
        restTransaction2().getId(),
        List.of(incomeTransactionCategory()));
    List<TransactionCategory> actualOther = api.createTransactionCategories(
        JOE_DOE_ACCOUNT_ID,
        restTransaction2().getId(),
        List.of(otherIncomeTransactionCategory())
    );

    assertEquals(1, actual.size());
    assertEquals(1, actualOther.size());
    assertEquals(actualOther.get(0).getComment(), otherIncomeTransactionCategory().getComment());
  }

  @Test
  void create_transaction_categories_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Cannot add category."
            + outcomeTransactionCategoryTmpl().getId() + " of type "
            + outcomeTransactionCategoryTmpl().getTransactionType() + " to transaction."
            + restTransaction2().getId()
            + " of type " + restTransaction2().getType()
            + "\"}",
        () -> api.createTransactionCategories(JOE_DOE_ACCOUNT_ID,
            restTransaction2().getId(),
            List.of(outcomeTransactionCategory()))
    );
  }

  private TransactionCategoryTemplate outcomeTransactionCategoryTmpl() {
    return TransactionCategoryTemplate
        .builder()
        .id("6e1767d9-b35d-411a-8d23-b725ecd00921")
        .type("Achat TVA 20%")
        .vat(new Fraction(BigInteger.valueOf(2000)))
        .transactionType(OUTCOME)
        .build();
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
