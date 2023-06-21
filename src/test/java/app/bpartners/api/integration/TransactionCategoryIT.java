package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.bridge.repository.BridgeTransactionRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.endpoint.rest.model.TransactionTypeEnum.INCOME;
import static app.bpartners.api.endpoint.rest.model.TransactionTypeEnum.OUTCOME;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.TRANSACTION1_ID;
import static app.bpartners.api.integration.conf.TestUtils.UNKNOWN_TRANSACTION_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.restTransaction1;
import static app.bpartners.api.integration.conf.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TransactionCategoryIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionCategoryIT {
  @MockBean
  private PaymentScheduleService paymentScheduleService;
  @MockBean
  private BuildingPermitConf buildingPermitConf;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private BridgeApi bridgeApi;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private AccountConnectorRepository accountConnectorRepository;
  @MockBean
  private BridgeTransactionRepository bridgeTransactionRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private CognitoComponent cognitoComponentMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  TransactionCategory fraisDeboursIncome() {
    return new TransactionCategory()
        .id("784f5f33-78bc-41c0-9782-0c8dd7de5956")
        .type("Frais débours")
        .transactionType(INCOME)
        .description("Encaisser frais débours")
        .count(0L)
        .isOther(false)
        .comment(null)
        .vat(0);
  }

  TransactionCategory fraisDeboursOutcome() {
    return new TransactionCategory()
        .id("564f5f33-78bc-41d0-9782-0c8dd7de8514")
        .type("Frais débours")
        .transactionType(OUTCOME)
        .description("Decaisser frais débours")
        .count(0L)
        .isOther(false)
        .comment(null)
        .vat(0);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);

    when(bridgeTransactionRepositoryMock.findById(any()))
        .thenReturn(BridgeTransaction.builder().build());
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
        .vat(1960)
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

    assertEquals(37, actualAll.size());
    assertEquals(actualIncome.get(13), fraisDeboursIncome());
    assertTrue(actualIncome.contains(fraisDeboursIncome()));
    assertTrue(actualOutcome.contains(fraisDeboursOutcome()));
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

    assertEquals(37, actualAll.size());
    assertTrue(actualAll.stream().noneMatch(e -> e.getCount() != 0L));
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void create_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actual = api.createTransactionCategories(JOE_DOE_ACCOUNT_ID,
        TRANSACTION1_ID,
        List.of(incomeTransactionCategory()));
    List<TransactionCategory> actualOther = api.createTransactionCategories(
        JOE_DOE_ACCOUNT_ID,
        TRANSACTION1_ID,
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
            + TRANSACTION1_ID
            + " of type " + restTransaction1().getType()
            + "\"}",
        () -> api.createTransactionCategories(JOE_DOE_ACCOUNT_ID,
            TRANSACTION1_ID,
            List.of(outcomeTransactionCategory()))
    );
    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"Transaction." + UNKNOWN_TRANSACTION_ID
            + " not found\"}",
        () -> api.createTransactionCategories(JOE_DOE_ACCOUNT_ID, UNKNOWN_TRANSACTION_ID,
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
