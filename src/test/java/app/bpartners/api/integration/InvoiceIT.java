package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.S3AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.service.InvoiceService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE2_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE3_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE4_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.createProduct4;
import static app.bpartners.api.integration.conf.TestUtils.createProduct5;
import static app.bpartners.api.integration.conf.TestUtils.customer1;
import static app.bpartners.api.integration.conf.TestUtils.customer2;
import static app.bpartners.api.integration.conf.TestUtils.product3;
import static app.bpartners.api.integration.conf.TestUtils.product4;
import static app.bpartners.api.integration.conf.TestUtils.product5;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = InvoiceIT.ContextInitializer.class)
@AutoConfigureMockMvc
class InvoiceIT {
  public static final String OTHER_ACCOUNT_ID = "other_account_id";
  public static final int MAX_PAGE_SIZE = 500;
  private static final String NEW_INVOICE_ID = "invoice_uuid";
  @Autowired
  private InvoiceService invoiceService;
  public static final String RANDOM_INVOICE_ID = "random_invoice_id";
  public static final String INVOICE5_ID = "invoice5_id";

  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
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
    return TestUtils.anApiClient(JOE_DOE_TOKEN, InvoiceIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
  }

  CrupdateInvoice proposalInvoice() {
    return new CrupdateInvoice()
        .ref("BP004")
        .title("Facture sans produit")
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .status(PROPOSAL)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .toPayAt(LocalDate.of(2022, 10, 13));
  }

  CrupdateInvoice draftInvoice() {
    return new CrupdateInvoice()
        .ref("BP005")
        .title("Facture achat")
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .status(DRAFT)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .toPayAt(LocalDate.of(2022, 11, 13));
  }

  CrupdateInvoice confirmedInvoice() {
    return new CrupdateInvoice()
        .ref("BP005")
        .title("Facture achat")
        .customer(customer1())
        .products(List.of(createProduct5()))
        .status(CONFIRMED)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .toPayAt(LocalDate.of(2022, 11, 13));
  }

  Invoice invoice1() {
    return new Invoice()
        .id(INVOICE1_ID)
        .fileId(INVOICE1_FILE_ID)
        .title("Facture tableau")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .customer(customer1()).ref("BP001")
        .sendingDate(LocalDate.of(2022, 9, 1))
        .toPayAt(LocalDate.of(2022, 10, 1))
        .status(CONFIRMED)
        .products(List.of(product3(), product4()))
        .totalPriceWithVat(8800)
        .totalVat(800)
        .totalPriceWithoutVat(8000);
  }

  Invoice invoice2() {
    return new Invoice()
        .id(INVOICE2_ID)
        .title("Facture plomberie")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .fileId("BP002.pdf")
        .customer(customer2().address("Nouvelle adresse"))
        .ref("BP002")
        .sendingDate(LocalDate.of(2022, 9, 10))
        .toPayAt(LocalDate.of(2022, 10, 10))
        .status(CONFIRMED)
        .products(List.of(product5()))
        .totalPriceWithVat(1100)
        .totalVat(100).totalPriceWithoutVat(1000);
  }

  CrupdateInvoice validInvoice() {
    return new CrupdateInvoice()
        .ref("BP003")
        .title("Facture sans produit")
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .status(DRAFT)
        .sendingDate(LocalDate.of(2022, 9, 10))
        .toPayAt(LocalDate.of(2022, 9, 11));
  }

  Invoice expectedDraft() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .fileId("BP003-TMP.pdf")
        .ref(validInvoice().getRef() + "-TMP")
        .title("Facture sans produit")
        .customer(validInvoice().getCustomer())
        .status(DRAFT)
        .sendingDate(validInvoice().getSendingDate())
        .products(List.of(product4().id(null), product5().id(null)))
        .toPayAt(validInvoice().getToPayAt())
        .totalPriceWithVat(3300)
        .totalVat(300)
        .totalPriceWithoutVat(3000);
  }

  Invoice expectedConfirmed() {
    return new Invoice()
        .id(INVOICE4_ID)
        .fileId(confirmedInvoice().getRef() + ".pdf")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .ref(confirmedInvoice().getRef())
        .title(confirmedInvoice().getTitle())
        .customer(confirmedInvoice().getCustomer())
        .status(CONFIRMED)
        .sendingDate(confirmedInvoice().getSendingDate())
        .products(List.of(product5().id(null)))
        .toPayAt(confirmedInvoice().getToPayAt())
        .totalPriceWithVat(1100)
        .totalVat(100)
        .totalPriceWithoutVat(1000);
  }

  Invoice expectedProposal() {
    return new Invoice()
        .id(INVOICE3_ID)
        .fileId("BP004-TMP.pdf")
        .ref(proposalInvoice().getRef() + "-TMP")
        .title("Facture sans produit")
        .customer(proposalInvoice().getCustomer())
        .status(PROPOSAL)
        .sendingDate(proposalInvoice().getSendingDate())
        .products(List.of(product4().id(null), product5().id(null)))
        .toPayAt(proposalInvoice().getToPayAt())
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
    List<Invoice> actual = api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, 10);

    assertEquals(invoice1(), actual1);
    assertEquals(invoice2(), actual2);
    assertTrue(actual.containsAll(List.of(actual1, actual2)));
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
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Required request parameter 'page' for method"
            + " parameter type PageFromOne is not present"
            + "\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, null, 10));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Required request parameter 'pageSize' for "
            + "method parameter type BoundedPageSize is not present\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, null));
  }

  @Test
  void crupdate_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actualDraft = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID, validInvoice());
    actualDraft.setProducts(ignoreIdsOf(actualDraft.getProducts()));
    Invoice actualProposal =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE3_ID, proposalInvoice());
    actualProposal.setProducts(ignoreIdsOf(actualProposal.getProducts()));
    Invoice actualConfirmed =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE4_ID, confirmedInvoice());
    actualConfirmed.setProducts(ignoreIdsOf(actualConfirmed.getProducts()));

    assertEquals(expectedDraft(), actualDraft);
    assertEquals(expectedProposal(), actualProposal);
    assertEquals(expectedConfirmed(), actualConfirmed);
    assertTrue(actualDraft.getRef().contains("TMP"));
    assertTrue(actualProposal.getRef().contains("TMP"));
    assertFalse(actualConfirmed.getRef().contains("TMP"));
  }

  @Test
  void crupdate_invoice_proposal_to_draft_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.random_invoice_id does not exist yet"
            + " and can only have DRAFT status\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, RANDOM_INVOICE_ID, proposalInvoice()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.invoice1_id was already confirmed\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, draftInvoice()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.invoice1_id was already confirmed\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, proposalInvoice()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.invoice5_id was already sent and "
            + "can not be modified anymore\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE5_ID, confirmedInvoice()));
  }

/* /!\ For local test only
  @Test
  void generate_invoice_pdf_ok() throws IOException {
    app.bpartners.api.model.Invoice invoice = app.bpartners.api.model.Invoice.builder()
        .id(INVOICE1_ID)
        .ref("invoice_ref")
        .title("invoice_title")
        .sendingDate(LocalDate.now())
        .toPayAt(LocalDate.now())
        .account(Account.builder()
            .id(JOE_DOE_ACCOUNT_ID)
            .iban("FR7630001007941234567890185")
            .bic("BPFRPP751")
            .build())
        .products(List.of(app.bpartners.api.model.Product.builder()
            .id("product_id")
            .quantity(50)
            .description("product description")
            .vatPercent(20)
            .unitPrice(150)
            .build()))
        .invoiceCustomer(InvoiceCustomer.customerTemplateBuilder()
            .name("Olivier Durant")
            .phone("+33 6 12 45 89 76")
            .email("exemple@email.com")
            .address("Paris 745")
            .build())
        .build();
    byte[] data = invoiceService.generateInvoicePdf(invoice);
    File generatedFile = new File("invoice.pdf");
    OutputStream os = new FileOutputStream(generatedFile);
    os.write(data);
    os.close();
  }

  @Test
  void generate_draft_pdf_ok() throws IOException {
    app.bpartners.api.model.Invoice invoice = app.bpartners.api.model.Invoice.builder()
        .id("draft_id")
        .ref("draft_ref")
        .title("draft_title")
        .sendingDate(LocalDate.now())
        .toPayAt(LocalDate.now())
        .account(Account.builder()
            .id(JOE_DOE_ACCOUNT_ID)
            .build())
        .products(List.of(app.bpartners.api.model.Product.builder()
            .id("product_id")
            .quantity(50)
            .description("product description")
            .vatPercent(20)
            .unitPrice(150)
            .build()))
        .invoiceCustomer(InvoiceCustomer.customerTemplateBuilder()
            .name("Olivier Durant")
            .phone("+33 6 12 45 89 76")
            .email("exemple@email.com")
            .address("Paris 745")
            .build())
        .build();
    byte[] data = invoiceService.generateDraftPdf(invoice);
    File generatedFile = new File("draft.pdf");
    OutputStream os = new FileOutputStream(generatedFile);
    os.write(data);
    os.close();
  }*/

  private List<Product> ignoreIdsOf(List<Product> actual) {
    return actual.stream()
        .peek(product -> product.setId(null))
        .collect(Collectors.toUnmodifiableList());
  }

  static class ContextInitializer extends S3AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
