package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.MonthlyTransactionsSummary;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionInvoice;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.model.TransactionsSummary;
import app.bpartners.api.endpoint.rest.security.bridge.BridgeConf;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.bridge.repository.BridgeTransactionRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HTransaction;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JANE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.TRANSACTION1_ID;
import static app.bpartners.api.integration.conf.TestUtils.UNKNOWN_TRANSACTION_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.invoice1;
import static app.bpartners.api.integration.conf.TestUtils.isAfterOrEquals;
import static app.bpartners.api.integration.conf.TestUtils.restTransaction1;
import static app.bpartners.api.integration.conf.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.Calendar.DECEMBER;
import static java.util.Calendar.JANUARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TransactionIT.ContextInitializer.class)
@AutoConfigureMockMvc
class TransactionIT {
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
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private BridgeConf bridgeConf;
  @MockBean
  private AccountConnectorRepository accountConnectorRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private BridgeApi bridgeApiMock;
  @MockBean
  private CognitoComponent cognitoComponentMock;
  @MockBean
  private BridgeTransactionRepository bridgeTransactionRepositoryMock;
  @MockBean
  private TransactionJpaRepository transactionJpaRepositoryMock;

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
        .income(1356000)
        .outcome(1050)
        .cashFlow(1354950);
  }

  MonthlyTransactionsSummary month2() {
    return new MonthlyTransactionsSummary()
        .id("monthly_transactions_summary2_id")
        .month(DECEMBER)
        .income(0)
        .outcome(0)
        .cashFlow(1354950);
  }

  TransactionsSummary transactionsSummary1() {
    int month1CashFlow = month1().getIncome() - month1().getOutcome();
    int month2CashFlow = month2().getIncome() - month2().getOutcome();
    return new TransactionsSummary()
        .year(LocalDate.now().getYear())
        .annualIncome(month1().getIncome() + month2().getIncome())
        .annualOutcome(month1().getOutcome() + month2().getOutcome())
        .annualCashFlow(month1CashFlow + month2CashFlow)
        .summary(List.of(month1(), month2()));
  }

  private static BridgeTransaction bridgeTransaction1() {
    return BridgeTransaction.builder()
        .id(1L)
        .label("Transaction 1")
        .amount(100.0)
        .transactionDate(LocalDate.of(2023, 1, 1))
        .build();
  }

  private static BridgeTransaction bridgeTransaction2() {
    return BridgeTransaction.builder()
        .id(2L)
        .label("Transaction 2")
        .amount(200.0)
        .transactionDate(LocalDate.of(2023, 1, 2))
        .build();
  }

  private static BridgeTransaction bridgeTransaction3() {
    return BridgeTransaction.builder()
        .id(3L)
        .label("Transaction 3")
        .amount(300.0)
        .transactionDate(LocalDate.of(2023, 1, 3))
        .build();
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    when(bridgeApiMock.findTransactionsUpdatedByToken(any()))
        .thenReturn(List.of());
  }

  private static HTransaction jpaTransactionEntity1() {
    return HTransaction.builder()
        .id("transaction1_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(null)
        .label("Création de site vitrine")
        .reference("REF_001")
        .amount("50000/1")
        .currency("EUR")
        .side("CREDIT_SIDE")
        .status(TransactionStatus.PENDING)
        .paymentDateTime(Instant.parse("2022-08-26T06:33:50.595Z"))
        .build();
  }

  private static HTransaction jpaTransactionEntity2() {
    return HTransaction.builder()
        .id("transaction2_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(null)
        .label("Premier virement")
        .reference("JOE-001")
        .amount("50000/1")
        .currency("EUR")
        .side("CREDIT_SIDE")
        .status(TransactionStatus.BOOKED)
        .paymentDateTime(Instant.parse("2022-08-24T03:39:33.315Z"))
        .build();
  }


  private static HTransaction bridgeTransactionEntity1() {
    return HTransaction.builder()
        .id("bridge_transaction1_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(bridgeTransaction1().getId())
        .label(bridgeTransaction1().getLabel())
        .amount(String.valueOf(parseFraction(bridgeTransaction2().getAmount())))
        .currency(bridgeTransaction1().getCurrency())
        .side(bridgeTransaction1().getSide())
        .status(bridgeTransaction1().getStatus())
        .paymentDateTime(bridgeTransaction1().getTransactionDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant())
        .build();
  }

  private static HTransaction bridgeTransactionEntity2() {
    return HTransaction.builder()
        .id("bridge_transaction2_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(bridgeTransaction2().getId())
        .label(bridgeTransaction2().getLabel())
        .amount(String.valueOf(parseFraction(bridgeTransaction2().getAmount())))
        .currency(bridgeTransaction2().getCurrency())
        .side(bridgeTransaction2().getSide())
        .status(bridgeTransaction2().getStatus())
        .paymentDateTime(bridgeTransaction2().getTransactionDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()).build();
  }

  private static HTransaction bridgeTransactionEntity3() {
    return HTransaction.builder()
        .id("bridge_transaction3_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(bridgeTransaction3().getId())
        .label(bridgeTransaction3().getLabel())
        .amount(String.valueOf(parseFraction(bridgeTransaction3().getAmount())))
        .currency(bridgeTransaction3().getCurrency())
        .side(bridgeTransaction3().getSide())
        .status(bridgeTransaction3().getStatus())
        .paymentDateTime(bridgeTransaction3().getTransactionDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant())
        .build();
  }

  @Test
  void read_transactions_twice_ok() throws ApiException {
    reset(transactionJpaRepositoryMock);
    when(bridgeTransactionRepositoryMock.findByBearer(JOE_DOE_TOKEN))
        .thenReturn(List.of(bridgeTransaction1(), bridgeTransaction2(), bridgeTransaction3()));
    when(transactionJpaRepositoryMock.findAllByIdBridge(bridgeTransaction1().getId())).thenReturn(
        List.of(bridgeTransactionEntity1()));
    when(transactionJpaRepositoryMock.findAllByIdBridge(bridgeTransaction2().getId())).thenReturn(
        List.of(bridgeTransactionEntity2()));
    when(transactionJpaRepositoryMock.findAllByIdBridge(bridgeTransaction3().getId())).thenReturn(
        List.of(bridgeTransactionEntity3()));
    List<HTransaction> mockedBridgeTransactions = List.of(
        bridgeTransactionEntity1(),
        bridgeTransactionEntity2(),
        bridgeTransactionEntity3());
    when(transactionJpaRepositoryMock.saveAll(any()))
        .thenReturn(mockedBridgeTransactions);
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actual1 = api.getTransactions(JOE_DOE_ACCOUNT_ID, null, null);
    List<Transaction> actual2 = api.getTransactions(JOE_DOE_ACCOUNT_ID, null, null);

    assertEquals(3, actual1.size());
    assertEquals(actual1, actual2);
    assertTrue(isAfterOrEquals(
        actual1.get(0).getPaymentDatetime(), actual1.get(1).getPaymentDatetime()));
    assertTrue(isAfterOrEquals(
        actual1.get(1).getPaymentDatetime(), actual1.get(2).getPaymentDatetime()));
    //TODO : actual transactions contains rest resource
  }

  @Test
  void read_transaction_by_id_ok() throws ApiException {
    reset(transactionJpaRepositoryMock);
    when(transactionJpaRepositoryMock.findById(jpaTransactionEntity1().getId())).thenReturn(
        Optional.of(jpaTransactionEntity1()));
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Transaction actual = api.getTransactionById(JOE_DOE_ACCOUNT_ID, TRANSACTION1_ID);

    assertEquals(restTransaction1(), actual);
  }

  @Test
  void read_transaction_by_id_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException("{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "Transaction.unknown_transaction_id is not found.\"}",
        () -> api.getTransactionById(JOE_DOE_ACCOUNT_ID, UNKNOWN_TRANSACTION_ID));
    assertThrowsForbiddenException(
        () -> api.getTransactionById(JANE_ACCOUNT_ID, TRANSACTION1_ID));
  }


  /*
  TODO: return empty when neither swan nor bridge return transaction
   */
  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void read_empty_transactions_ok() throws ApiException {
    reset(bridgeTransactionRepositoryMock);
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actual = api.getTransactions(JOE_DOE_ACCOUNT_ID, null, null);

    assertTrue(actual.isEmpty());
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void justify_transaction_ok() throws ApiException {
    reset(transactionJpaRepositoryMock);
    when(transactionJpaRepositoryMock.findById(jpaTransactionEntity1().getId())).thenReturn(
        Optional.of(jpaTransactionEntity1()));
    when(transactionJpaRepositoryMock.save(any())).thenReturn(jpaTransactionEntity1().toBuilder()
        .invoice(HInvoice.builder()
            .id(INVOICE1_ID)
            .fileId("file1_id")
            .build())
        .build());
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    Transaction transaction1 = restTransaction1();
    Invoice invoice1 = invoice1();

    Transaction actual = api.justifyTransaction(
        JOE_DOE_ACCOUNT_ID, transaction1.getId(), invoice1.getId());

    assertEquals(
        transaction1
            .invoice(new TransactionInvoice()
                .invoiceId(invoice1.getId())
                .fileId(invoice1.getFileId())),
        actual);
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
    assertEquals(transactionsSummary1()
            .updatedAt(actualDefaultYear.getUpdatedAt()),
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

  List<Transaction> ignoreIds(List<Transaction> actual) {
    actual.forEach(transaction -> {
      transaction.setId(null);
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
