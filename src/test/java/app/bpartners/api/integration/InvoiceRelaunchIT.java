package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.endpoint.rest.model.EmailInfo;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunch;
import app.bpartners.api.endpoint.rest.model.RelaunchType;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE3_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE7_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE_RELAUNCH1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE_RELAUNCH2_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.OTHER_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.invoice1;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = InvoiceRelaunchIT.ContextInitializer.class)
@AutoConfigureMockMvc
class InvoiceRelaunchIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private SwanConf swanConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private AccountHolderSwanRepository accountHolderRepositoryMock;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @MockBean
  private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        InvoiceRelaunchIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  InvoiceRelaunch invoiceRelaunch1() {
    return new InvoiceRelaunch()
        .id(INVOICE_RELAUNCH1_ID)
        .type(RelaunchType.PROPOSAL)
        .invoice(invoice1().fileId(null))
        .accountId(JOE_DOE_ACCOUNT_ID)
        .isUserRelaunched(true)
        .emailInfo(new EmailInfo())
        .creationDatetime(Instant.parse("2022-01-01T01:00:00.00Z"));
  }

  InvoiceRelaunch invoiceRelaunch2() {
    return new InvoiceRelaunch()
        .id(INVOICE_RELAUNCH2_ID)
        .type(RelaunchType.CONFIRMED)
        .invoice(invoice1().fileId(null))
        .accountId(JOE_DOE_ACCOUNT_ID)
        .isUserRelaunched(false)
        .emailInfo(new EmailInfo())
        .creationDatetime(Instant.parse("2022-01-01T01:00:00.00Z"));
  }

  CreateInvoiceRelaunch creatableInvoiceRelaunch() {
    return new CreateInvoiceRelaunch()
        .subject("relaunch_object")
        .message("<p>Email body</p>");
  }

  CreateInvoiceRelaunch otherCreatableInvoiceRelaunch() {
    return new CreateInvoiceRelaunch()
        ._object("relaunch_object")
        .emailBody("<p>Email body</p>");
  }

  InvoiceRelaunch expectedRelaunch() {
    return new InvoiceRelaunch()
        .invoice(invoice1().fileId(null))
        .type(RelaunchType.CONFIRMED)
        .accountId(JOE_DOE_ACCOUNT_ID)
        .emailInfo(new EmailInfo()
            .emailObject("[NUMER] relaunch_object")
            .emailBody("<p>Email body</p>"))
        .isUserRelaunched(true);
  }

  @Test
  void relaunch_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    InvoiceRelaunch actual =
        api.relaunchInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, creatableInvoiceRelaunch());
    actual.setInvoice(actual.getInvoice().updatedAt(null));
    InvoiceRelaunch otherActual =
        api.relaunchInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, otherCreatableInvoiceRelaunch());
    otherActual.setInvoice(otherActual.getInvoice().updatedAt(null));

    assertEquals(
        expectedRelaunch()
            .id(actual.getId())
            .creationDatetime(actual.getCreationDatetime())
        , actual);
    assertEquals(
        expectedRelaunch()
            .id(otherActual.getId())
            .creationDatetime(otherActual.getCreationDatetime()),
        otherActual
    );
  }

  @Test
  void read_invoice_relaunches_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<InvoiceRelaunch> actual =
        api.getRelaunches(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, 1, 20, null);
    List<InvoiceRelaunch> proposals =
        api.getRelaunches(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, 1, 20,
            RelaunchType.PROPOSAL.toString());
    List<InvoiceRelaunch> confirmed =
        api.getRelaunches(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, 1, 20,
            RelaunchType.CONFIRMED.toString());
    List<InvoiceRelaunch> actualWithoutUpdatedDate = ignoreUpdatedDate(actual);
    List<InvoiceRelaunch> proposalsWithoutUpdatedDate = ignoreUpdatedDate(proposals);
    List<InvoiceRelaunch> confirmedWithoutUpdatedDate = ignoreUpdatedDate(confirmed);

    assertTrue(actualWithoutUpdatedDate.contains(invoiceRelaunch1()));
    assertFalse(confirmedWithoutUpdatedDate.contains(invoiceRelaunch1()));
    assertTrue(proposalsWithoutUpdatedDate.contains(invoiceRelaunch1()));
    assertTrue(actualWithoutUpdatedDate.contains(invoiceRelaunch2()));
    assertFalse(proposalsWithoutUpdatedDate.contains(invoiceRelaunch2()));
    assertTrue(confirmedWithoutUpdatedDate.contains(invoiceRelaunch2()));
  }

  @Test
  void read_invoice_relaunches_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.getRelaunches(OTHER_ACCOUNT_ID, INVOICE1_ID, 1, 20, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Type value should be PROPOSAL or CONFIRMED\"}",
        () -> api.getRelaunches(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, 1, 20, "DRAFT")
    );
  }

  @Test
  void relaunch_invoice_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.relaunchInvoice(OTHER_ACCOUNT_ID, INVOICE1_ID, creatableInvoiceRelaunch()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.invoice3_id actual status is"
            + " DRAFT and it cannot be relaunched\"}",
        () -> api.relaunchInvoice(JOE_DOE_ACCOUNT_ID, INVOICE3_ID, creatableInvoiceRelaunch()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.invoice7_id actual status is"
            + " PAID and it cannot be relaunched\"}",
        () -> api.relaunchInvoice(JOE_DOE_ACCOUNT_ID, INVOICE7_ID, creatableInvoiceRelaunch()));
  }

  private List<InvoiceRelaunch> ignoreUpdatedDate(List<InvoiceRelaunch> list) {
    return list.stream()
        .peek(invoiceRelaunch -> invoiceRelaunch.getInvoice().setUpdatedAt(null))
        .collect(Collectors.toUnmodifiableList());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
