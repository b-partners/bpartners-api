package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
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
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE2_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.customer1;
import static app.bpartners.api.integration.conf.TestUtils.invoice1;
import static app.bpartners.api.integration.conf.TestUtils.invoice2;
import static app.bpartners.api.integration.conf.TestUtils.product4;
import static app.bpartners.api.integration.conf.TestUtils.product5;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = InvoiceIT.ContextInitializer.class)
@AutoConfigureMockMvc
class InvoiceIT {
  public static final String OTHER_ACCOUNT_ID = "other_account_id";
  public static final int MAX_PAGE_SIZE = 500;
  private static final String NEW_INVOICE_ID = "invoice_uuid";
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
  private AccountHolderSwanRepository accountHolderRepositoryMock;
  @MockBean
  private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, InvoiceIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
  }

  CrupdateInvoice validInvoice() {
    return new CrupdateInvoice()
        .ref("BP003")
        .title("Facture sans produit")
        .customer(customer1())
        .products(List.of(product4(), product5()))
        .sendingDate(LocalDate.of(2022, 9, 10))
        .toPayAt(LocalDate.of(2022, 9, 11));
  }

  Invoice createdInvoice() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .fileId("BP003.pdf")
        .ref(validInvoice().getRef())
        .title("Facture sans produit")
        .customer(validInvoice().getCustomer())
        .status(InvoiceStatus.CONFIRMED)
        .sendingDate(validInvoice().getSendingDate())
        .products(List.of(product4(), product5()))
        .toPayAt(validInvoice().getToPayAt())
        .totalPriceWithVat(3300)
        .totalVat(300)
        .totalPriceWithoutVat(3000);
  }

  @Test
  void read_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actual1 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE1_ID);
    Invoice actual2 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE2_ID);
    //TODO: debug duplicated entry on duplicated invoiceCustomer for crupdated invoice
    // List<Invoice> actual = api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, 10);

    assertEquals(invoice1(), actual1);
    assertEquals(invoice2(), actual2);
    //assertTrue(actual.containsAll(List.of(actual1, actual2)));
  }

  @Test
  void read_invoice_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getInvoices(OTHER_ACCOUNT_ID, 1, 10));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page must be >=1\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, -1, 10));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page size must be >=1\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, -10));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page size must be <" + MAX_PAGE_SIZE
            + "\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, MAX_PAGE_SIZE + 1));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page is mandatory. page_size is mandatory. "
            + "\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, null, null));
  }

  @Test
  void crupdate_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actual = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID, validInvoice());

    assertEquals(createdInvoice(), actual.paymentUrl(null));
  }

  /* /!\ Use for unit test only
  @Test
  void generate_invoice_pdf_ok() throws IOException {
    byte[] data = invoiceService.generateInvoicePdf(INVOICE1_ID);
    File generatedFile = new File("test.pdf");
    OutputStream os = new FileOutputStream(generatedFile);
    os.write(data);
    os.close();
  }
*/

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
