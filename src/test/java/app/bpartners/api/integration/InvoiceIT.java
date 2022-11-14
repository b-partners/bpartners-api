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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE2_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE3_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE4_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE7_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.NOT_JOE_DOE_ACCOUNT_ID;
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
import static app.bpartners.api.integration.conf.TestUtils.setUpEventBridge;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = InvoiceIT.ContextInitializer.class)
@AutoConfigureMockMvc
class InvoiceIT {
  public static final int MAX_PAGE_SIZE = 500;
  public static final String RANDOM_INVOICE_ID = "random_invoice_id";
  public static final String INVOICE5_ID = "invoice5_id";
  private static final String NEW_INVOICE_ID = "invoice_uuid";
  @Autowired
  private InvoiceService invoiceService;
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
  @MockBean
  private EventBridgeClient eventBridgeClientMock;

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
    setUpEventBridge(eventBridgeClientMock);
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

  CrupdateInvoice paidInvoice() {
    return new CrupdateInvoice()
        .ref("BP009")
        .title("Facture transaction")
        .customer(customer1())
        .products(List.of(createProduct5()))
        .status(PAID)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .toPayAt(LocalDate.of(2022, 11, 10));
  }

  Invoice invoice1() {
    return new Invoice()
        .id(INVOICE1_ID)
        .comment("Tableau de Madagascar")
        .title("Facture tableau")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .customer(customer1()).ref("BP001")
        .sendingDate(LocalDate.of(2022, 9, 1))
        .toPayAt(LocalDate.of(2022, 10, 1))
        .status(CONFIRMED)
        .products(List.of(product3(), product4()))
        .totalPriceWithVat(8800.0)
        .totalVat(800.0)
        .totalPriceWithoutVat(8000.0);
  }

  Invoice invoice2() {
    return new Invoice()
        .id(INVOICE2_ID)
        .title("Facture plomberie")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .customer(customer2().address("Nouvelle adresse"))
        .ref("BP002")
        .sendingDate(LocalDate.of(2022, 9, 10))
        .toPayAt(LocalDate.of(2022, 10, 10))
        .status(CONFIRMED)
        .products(List.of(product5()))
        .totalPriceWithVat(1100.0)
        .totalVat(100.0).totalPriceWithoutVat(1000.0);
  }

  Invoice invoice6() {
    return new Invoice()
        .id("invoice6_id")
        .paymentUrl(null)
        .comment(null)
        .ref("BP007-TMP")
        .title("Facture transaction")
        .customer(customer1())
        .status(DRAFT)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .products(List.of())
        .toPayAt(LocalDate.of(2022, 11, 10))
        .totalPriceWithVat(0.0)
        .totalVat(0.0)
        .totalPriceWithoutVat(0.0);
  }

  CrupdateInvoice validInvoice() {
    return new CrupdateInvoice()
        .ref("BP003")
        .title("Facture sans produit")
        .comment("Nouveau commentaire")
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .status(DRAFT)
        .sendingDate(LocalDate.now())
        .toPayAt(LocalDate.now().plusDays(1L));
  }

  Invoice expectedDraft() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .comment(validInvoice().getComment())
        .ref(validInvoice().getRef() + "-TMP")
        .title("Facture sans produit")
        .customer(validInvoice().getCustomer())
        .status(DRAFT)
        .sendingDate(validInvoice().getSendingDate())
        .products(List.of(product4().id(null), product5().id(null)))
        .toPayAt(validInvoice().getToPayAt())
        .totalPriceWithVat(3300.0)
        .totalVat(300.0)
        .totalPriceWithoutVat(3000.0);
  }

  Invoice expectedConfirmed() {
    return new Invoice()
        .id(INVOICE4_ID)
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .ref(confirmedInvoice().getRef())
        .title(confirmedInvoice().getTitle())
        .customer(confirmedInvoice().getCustomer())
        .status(CONFIRMED)
        .sendingDate(confirmedInvoice().getSendingDate())
        .products(List.of(product5().id(null)))
        .toPayAt(confirmedInvoice().getToPayAt())
        .totalPriceWithVat(1100.0)
        .totalVat(100.0)
        .totalPriceWithoutVat(1000.0);
  }

  Invoice expectedInitializedDraft() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .products(List.of())
        .totalVat(0.0)
        .totalPriceWithoutVat(0.0)
        .totalPriceWithVat(0.0)
        .status(DRAFT);
  }

  Invoice expectedPaid() {
    return new Invoice()
        .id(INVOICE4_ID)
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .ref(paidInvoice().getRef())
        .title(paidInvoice().getTitle())
        .customer(paidInvoice().getCustomer())
        .status(PAID)
        .sendingDate(paidInvoice().getSendingDate())
        .products(List.of(product5().id(null)))
        .toPayAt(paidInvoice().getToPayAt())
        .totalPriceWithVat(1100.0)
        .totalVat(100.0)
        .totalPriceWithoutVat(1000.0);
  }

  //TODO: create PaginationIT for pagination test and add filters.
  // In particular, check the date filters and the order filters (by created datetime desc)
  @Test
  void read_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actual1 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE1_ID);
    Invoice actual2 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE2_ID);
    List<Invoice> actualDraft = api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, 10, DRAFT);
    List<Invoice> actualNotFiltered = api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, 10, null);

    assertEquals(invoice1(), actual1.updatedAt(null));
    assertEquals(invoice2(), actual2.updatedAt(null));
    assertTrue(ignoreUpdatedAt(actualDraft).contains(invoice6()));
    assertTrue(ignoreUpdatedAt(actualNotFiltered).containsAll(
        List.of(actual1.updatedAt(null),
            actual2.updatedAt(null),
            invoice6().updatedAt(null))));
  }

  @Test
  void read_invoice_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getInvoices(NOT_JOE_DOE_ACCOUNT_ID, 1, 10, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page must be >=1\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, -1, 10, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page size must be >=1\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, -10, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page size must be <" + MAX_PAGE_SIZE
            + "\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, MAX_PAGE_SIZE + 1, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Required request parameter 'page' for method"
            + " parameter type PageFromOne is not present"
            + "\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, null, 10, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Required request parameter 'pageSize' for "
            + "method parameter type BoundedPageSize is not present\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, null, null));
  }

  @Test
  void crupdate_invoice_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String firstInvoiceId = randomUUID().toString();
    Executable firstCrupdateExecutable =
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, firstInvoiceId,
            validInvoice().ref("unique_ref"));
    Executable secondCrupdateExecutable =
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, randomUUID().toString(),
            validInvoice().ref("unique_ref"));

    assertDoesNotThrow(firstCrupdateExecutable);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
            + "The invoice reference must unique however the given reference [unique_ref] is"
            + " already used by invoice." + firstInvoiceId + "\"}",
        secondCrupdateExecutable);
  }

  // /!\ It seems that the localstack does not support the SES service using the default credentials
  // So note that SES service is mocked and do nothing for this test
  @Test
  void crupdate_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actualDraft = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID,
        initializeDraft());
    Invoice actualUpdatedDraft = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID,
        validInvoice());
    actualUpdatedDraft.setProducts(ignoreIdsOf(actualUpdatedDraft.getProducts()));
    Invoice actualConfirmed =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE4_ID, confirmedInvoice());
    actualConfirmed.setProducts(ignoreIdsOf(actualConfirmed.getProducts()));
    Invoice actualPaid = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE4_ID, paidInvoice());
    actualPaid.setProducts(ignoreIdsOf(actualPaid.getProducts()));

    assertEquals(expectedInitializedDraft(), actualDraft);
    assertEquals(expectedDraft(), actualUpdatedDraft);
    assertEquals(expectedConfirmed(), actualConfirmed);
    assertEquals(expectedPaid(), actualPaid);
    assertTrue(actualUpdatedDraft.getRef().contains("TMP"));
    assertFalse(actualConfirmed.getRef().contains("TMP"));
  }

  @Test
  void crupdate_triggers_event_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    reset(eventBridgeClientMock);
    when(eventBridgeClientMock.putEvents((PutEventsRequest) any())).thenReturn(
        PutEventsResponse.builder().entries(
                PutEventsResultEntry.builder().eventId("eventId1").build())
            .build());

    Invoice actualProposal =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE3_ID, proposalInvoice());

    ArgumentCaptor<PutEventsRequest> captor = ArgumentCaptor.forClass(PutEventsRequest.class);
    verify(eventBridgeClientMock, times(1)).putEvents(captor.capture());
    PutEventsRequest actualRequest = captor.getValue();
    List<PutEventsRequestEntry> actualRequestEntries = actualRequest.entries();
    assertEquals(1, actualRequestEntries.size());
    PutEventsRequestEntry fileUploadEvent = actualRequestEntries.get(0);
    assertTrue(fileUploadEvent.detail().contains(actualProposal.getId()));
    assertTrue(fileUploadEvent.detail().contains(JOE_DOE_ACCOUNT_ID));
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
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.invoice1_id actual status is CONFIRMED"
            + " and can only become PAID\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, draftInvoice()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.invoice1_id actual status is CONFIRMED"
            + " and can only become PAID\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, proposalInvoice()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invoice.invoice7_id was already paid\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE7_ID, proposalInvoice()));
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

  private List<Invoice> ignoreUpdatedAt(List<Invoice> actual) {
    return actual.stream()
        .peek(invoice -> invoice.setUpdatedAt(null))
        .collect(Collectors.toUnmodifiableList());
  }

  private CrupdateInvoice initializeDraft() {
    return new CrupdateInvoice()
        .status(DRAFT);
  }

  static class ContextInitializer extends S3AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
