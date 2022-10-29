package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.model.TypedMailSent;
import app.bpartners.api.endpoint.event.model.gen.MailSent;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunch;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.S3AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.service.aws.SesService;
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

import static app.bpartners.api.integration.conf.TestUtils.BAD_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE3_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE_RELAUNCH1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE_RELAUNCH2_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.invoice1;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSesService;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
  private SesService sesServiceMock;
  @MockBean
  private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;

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
    setUpSesService(sesServiceMock);
  }

  InvoiceRelaunch invoiceRelaunch1() {
    return new InvoiceRelaunch()
        .id(INVOICE_RELAUNCH1_ID)
        .invoice(invoice1())
        .accountId(JOE_DOE_ACCOUNT_ID)
        .isUserRelaunched(true)
        .creationDatetime(Instant.parse("2022-01-01T01:00:00.00Z"));
  }

  InvoiceRelaunch invoiceRelaunch2() {
    return new InvoiceRelaunch()
        .id(INVOICE_RELAUNCH2_ID)
        .invoice(invoice1())
        .accountId(JOE_DOE_ACCOUNT_ID)
        .isUserRelaunched(false)
        .creationDatetime(Instant.parse("2022-01-01T01:00:00.00Z"));
  }

  InvoiceRelaunch expectedRelaunch() {
    return new InvoiceRelaunch()
        .invoice(invoice1())
        .accountId(JOE_DOE_ACCOUNT_ID)
        .isUserRelaunched(true);
  }

  @Test
  void relaunch_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    InvoiceRelaunch expected = expectedRelaunch();
    when(sesServiceMock.toTypedEvent(any(), any(), any(), any(), any())).thenReturn(
        typedMailSent());

    InvoiceRelaunch actual = api.relaunchInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, null);
    expected.setId(actual.getId());
    expected.setCreationDatetime(actual.getCreationDatetime());
    actual.getInvoice().setUpdatedAt(null);

    assertEquals(expected, actual);
  }

  @Test
  void read_invoice_relaunches_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<InvoiceRelaunch> actualNotHandmade = ignoreUpdatedDate(
        api.getRelaunches(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, false, 1, 20)
    );
    List<InvoiceRelaunch> actualHandmade = ignoreUpdatedDate(
        api.getRelaunches(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, true, 1, 20)
    );
    List<InvoiceRelaunch> actualAll = ignoreUpdatedDate(
        api.getRelaunches(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, null, 1, 20)
    );

    assertTrue(actualHandmade.contains(invoiceRelaunch1()));
    assertTrue(actualNotHandmade.contains(invoiceRelaunch2()));
    assertTrue(actualAll.containsAll(actualHandmade));
    assertTrue(actualAll.containsAll(actualNotHandmade));
  }

  @Test
  void read_and_relaunch_invoices_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.getRelaunches(BAD_ACCOUNT_ID, INVOICE1_ID, null, 1, 20));
    assertThrowsForbiddenException(
        () -> api.relaunchInvoice(BAD_ACCOUNT_ID, INVOICE1_ID, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.invoice3_id can not be sent because status is DRAFT\"}",
        () -> api.relaunchInvoice(JOE_DOE_ACCOUNT_ID, INVOICE3_ID, null)
    );
  }

  private List<InvoiceRelaunch> ignoreUpdatedDate(List<InvoiceRelaunch> list) {
    return list.stream()
        .peek(invoiceRelaunch -> invoiceRelaunch.getInvoice().setUpdatedAt(null))
        .collect(Collectors.toUnmodifiableList());
  }

  private TypedMailSent typedMailSent() {
    return new TypedMailSent(MailSent.builder()
        .subject("Mail subject")
        .attachmentAsBytes(null)
        .recipient("customer@bpartners.app")
        .attachmentName("Mail attachment name")
        .htmlBody("<html><body>Mail body</body></html>")
        .build());
  }

  static class ContextInitializer extends S3AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
