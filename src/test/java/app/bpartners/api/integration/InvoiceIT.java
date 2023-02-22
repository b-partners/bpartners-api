package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreatePaymentRegulation;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceDiscount;
import app.bpartners.api.endpoint.rest.model.PaymentRegulation;
import app.bpartners.api.endpoint.rest.model.PaymentRequest;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.S3AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.service.InvoiceService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

import static app.bpartners.api.endpoint.rest.model.CrupdateInvoice.PaymentTypeEnum.IN_INSTALMENT;
import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE2_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE3_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE4_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.NOT_JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.SWAN_ACCOUNTHOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.createProduct2;
import static app.bpartners.api.integration.conf.TestUtils.createProduct4;
import static app.bpartners.api.integration.conf.TestUtils.createProduct5;
import static app.bpartners.api.integration.conf.TestUtils.customer1;
import static app.bpartners.api.integration.conf.TestUtils.customer2;
import static app.bpartners.api.integration.conf.TestUtils.datedPaymentRequest1;
import static app.bpartners.api.integration.conf.TestUtils.datedPaymentRequest2;
import static app.bpartners.api.integration.conf.TestUtils.product3;
import static app.bpartners.api.integration.conf.TestUtils.product4;
import static app.bpartners.api.integration.conf.TestUtils.product5;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpEventBridge;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static app.bpartners.api.model.Invoice.DEFAULT_DELAY_PENALTY_PERCENT;
import static app.bpartners.api.model.Invoice.DEFAULT_TO_PAY_DELAY_DAYS;
import static java.util.UUID.randomUUID;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class InvoiceIT {
  public static final int MAX_PAGE_SIZE = 500;
  public static final String DRAFT_REF_PREFIX = "BROUILLON-";
  private static final String NEW_INVOICE_ID = "invoice_uuid";
  public static String PROPOSAL_REF_PREFIX = "DEVIS-";
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
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private AccountHolderJpaRepository holderJpaRepository;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, InvoiceIT.ContextInitializer.SERVER_PORT);
  }

  private static PaymentRegulation expectedDated2() {
    return new PaymentRegulation()
        .maturityDate(LocalDate.of(2023, 2, 15))
        .paymentRequest(new PaymentRequest()
            .reference("BP005")
            .payerName(customer1().getName())
            .payerEmail(customer1().getEmail())
            .paymentUrl("https://connect-v2-sbx.fintecture.com")
            .amount(1000)
            .label("Montant restant"));
  }

  private static PaymentRegulation expectedDated1() {
    return new PaymentRegulation()
        .maturityDate(LocalDate.of(2023, 2, 1))
        .paymentRequest(new PaymentRequest()
            .reference("BP005")
            .payerName(customer1().getName())
            .payerEmail(customer1().getEmail())
            .paymentUrl("https://connect-v2-sbx.fintecture.com")
            .amount(100)
            .label("Un euro"));
  }

  private static List<PaymentRegulation> ignoreIdsAndDatetime(Invoice actualConfirmed) {
    List<PaymentRegulation> paymentRegulations =
        new ArrayList<>(actualConfirmed.getPaymentRegulations());
    paymentRegulations.forEach(
        datedPaymentRequest -> datedPaymentRequest.setPaymentRequest(
            datedPaymentRequest.getPaymentRequest()
                .id(null)
                .initiatedDatetime(null)));
    return paymentRegulations;
  }
  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    when(holderJpaRepository.findByAccountId(JOE_DOE_ACCOUNT_ID))
        .thenReturn(Optional.of(accountHolderEntity1()));
  }

  public static Invoice invoice1() {
    return new Invoice()
        .id(INVOICE1_ID)
        .fileId("file1_id")
        .comment(null)
        .title("Outils pour plomberie")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .paymentType(Invoice.PaymentTypeEnum.IN_INSTALMENT)
        .paymentRegulations(List.of(datedPaymentRequest1(), datedPaymentRequest2()))
        .customer(customer1()).ref("BP001")
        .createdAt(Instant.parse("2022-01-01T01:00:00.00Z"))
        .sendingDate(LocalDate.of(2022, 9, 1))
        .validityDate(LocalDate.of(2022, 10, 3))
        .toPayAt(LocalDate.of(2022, 10, 1))
        .delayInPaymentAllowed(DEFAULT_TO_PAY_DELAY_DAYS)
        .delayPenaltyPercent(DEFAULT_DELAY_PENALTY_PERCENT)
        .status(CONFIRMED)
        .products(List.of(product3(), product4()))
        .totalPriceWithVat(8800)
        .totalVat(800)
        .totalPriceWithoutVat(8000)
        .totalPriceWithoutDiscount(8000)
        .globalDiscount(new InvoiceDiscount()
            .amountValue(0)
            .percentValue(0))
        .metadata(Map.of());
  }

  private static List<PaymentRegulation> initPaymentReg(String id) {
    return List.of(new PaymentRegulation()
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(new PaymentRequest()
                .paymentUrl(null)
                .reference(id)
                .amount(552)
                .payerName("Luc Artisan")
                .payerEmail("bpartners.artisans@gmail.com")
                .label("Acompte de 10%")),
        new PaymentRegulation()
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(new PaymentRequest()
                .paymentUrl(null)
                .amount(1648)
                .reference(id)
                .payerName("Luc Artisan")
                .payerEmail("bpartners.artisans@gmail.com")
                .label("Reste 90%")));
  }

  private static List<PaymentRegulation> updatedPaymentRegulations(String id) {
    return List.of(new PaymentRegulation()
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(new PaymentRequest()
                .paymentUrl(null)
                .reference(id)
                .amount(225)
                .payerName("Luc Artisan")
                .payerEmail("bpartners.artisans@gmail.com")
                .label("Acompte de 10%")),
        new PaymentRegulation()
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(new PaymentRequest()
                .paymentUrl(null)
                .amount(1975)
                .reference(id)
                .payerName("Luc Artisan")
                .payerEmail("bpartners.artisans@gmail.com")
                .label("Reste 90%")));
  }

  private static List<PaymentRegulation> confirmedPaymentRegulations(String id) {
    return List.of(new PaymentRegulation()
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(new PaymentRequest()
                .paymentUrl("https://connect-v2-sbx.fintecture.com")
                .reference(id)
                .amount(225)
                .payerName("Luc Artisan")
                .payerEmail("bpartners.artisans@gmail.com")
                .label("Acompte de 10%")),
        new PaymentRegulation()
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(new PaymentRequest()
                .paymentUrl("https://connect-v2-sbx.fintecture.com")
                .amount(1975)
                .reference(id)
                .payerName("Luc Artisan")
                .payerEmail("bpartners.artisans@gmail.com")
                .label("Reste 90%")));
  }

  private HAccountHolder accountHolderEntity1() {
    return HAccountHolder.builder()
        .id(SWAN_ACCOUNTHOLDER_ID)
        .accountId(JOE_DOE_ACCOUNT_ID)
        .mobilePhoneNumber("+33 6 11 22 33 44")
        .email("numer@hei.school")
        .socialCapital(40000)
        .vatNumber("FR 32 123456789")
        .initialCashflow("6000/1")
        .subjectToVat(true)
        .build();
  }

  CrupdateInvoice proposalInvoice() {
    return new CrupdateInvoice()
        .ref("BP004")
        .title("Facture sans produit")
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .status(PROPOSAL)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .validityDate(LocalDate.of(2022, 10, 14))
        .toPayAt(LocalDate.of(2022, 10, 13));
  }

  CrupdateInvoice confirmedInvoice() {
    return new CrupdateInvoice()
        .ref("BP005")
        .title("Facture achat")
        .customer(customer1())
        .products(List.of(createProduct5()))
        .paymentRegulations(List.of(new CreatePaymentRegulation()
                .maturityDate(LocalDate.of(2023, 2, 1))
                .amount(100)
                .percent(null)
                .comment("Un euro"),
            new CreatePaymentRegulation()
                .maturityDate(LocalDate.of(2023, 2, 15))
                .amount(1000)
                .percent(null)
                .comment("Montant restant")))
        .paymentType(IN_INSTALMENT)
        .status(CONFIRMED)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .validityDate(LocalDate.of(2022, 10, 14))
        .toPayAt(LocalDate.of(2022, 11, 13))
        .delayInPaymentAllowed(15)
        .delayPenaltyPercent(20);
  }

  CrupdateInvoice paidInvoice() {
    return new CrupdateInvoice()
        .ref("BP005")
        .title("Facture achat")
        .customer(customer1())
        .products(List.of(createProduct5()))
        .paymentType(IN_INSTALMENT)
        .status(PAID)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .validityDate(LocalDate.of(2022, 10, 14))
        .toPayAt(LocalDate.of(2022, 11, 13))
        .delayInPaymentAllowed(15)
        .delayPenaltyPercent(20);
  }

  CrupdateInvoice paidInvoice1() {
    return new CrupdateInvoice()
        .ref("BP006")
        .title("Facture achat")
        .customer(customer1())
        .products(List.of(createProduct5()))
        .paymentType(IN_INSTALMENT)
        .status(PAID)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .validityDate(LocalDate.of(2022, 10, 14))
        .toPayAt(LocalDate.of(2022, 11, 13))
        .delayInPaymentAllowed(15)
        .delayPenaltyPercent(20);
  }

  Invoice invoice2() {
    return new Invoice()
        .id(INVOICE2_ID)
        .title("Facture plomberie")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .customer(customer2())
        .ref("BP002")
        .paymentRegulations(List.of())
        .paymentType(CASH)
        .sendingDate(LocalDate.of(2022, 9, 10))
        .validityDate(LocalDate.of(2022, 10, 14))
        .createdAt(Instant.parse("2022-01-01T03:00:00.00Z"))
        .toPayAt(LocalDate.of(2022, 10, 10))
        .delayInPaymentAllowed(DEFAULT_TO_PAY_DELAY_DAYS)
        .delayPenaltyPercent(DEFAULT_DELAY_PENALTY_PERCENT)
        .status(CONFIRMED)
        .products(List.of(product5()))
        .totalPriceWithVat(1100)
        .totalVat(100)
        .totalPriceWithoutVat(1000)
        .totalPriceWithoutDiscount(1000)
        .globalDiscount(new InvoiceDiscount()
            .amountValue(0)
            .percentValue(0))
        .metadata(Map.of());
  }

  Invoice invoice6() {
    return new Invoice()
        .id("invoice6_id")
        .paymentUrl(null)
        .comment(null)
        .ref(DRAFT_REF_PREFIX + "BP007")
        .title("Facture transaction")
        .customer(customer1())
        .status(DRAFT)
        .createdAt(Instant.parse("2022-01-01T06:00:00Z"))
        .sendingDate(LocalDate.of(2022, 10, 12))
        .validityDate(LocalDate.of(2022, 11, 12))
        .delayInPaymentAllowed(DEFAULT_TO_PAY_DELAY_DAYS)
        .delayPenaltyPercent(DEFAULT_DELAY_PENALTY_PERCENT)
        .paymentRegulations(List.of())
        .paymentType(CASH)
        .toPayAt(LocalDate.of(2022, 11, 10))
        .products(List.of())
        .totalPriceWithVat(0)
        .totalPriceWithoutVat(0)
        .totalPriceWithoutDiscount(0)
        .totalVat(0)
        .globalDiscount(new InvoiceDiscount()
            .amountValue(0)
            .percentValue(0))
        .metadata(Map.of());
  }

  CrupdateInvoice validInvoice() {
    return initializeDraft()
        .ref("BP003")
        .title("Facture sans produit")
        .comment("Nouveau commentaire")
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .sendingDate(LocalDate.now())
        .validityDate(LocalDate.now().plusDays(3L))
        .globalDiscount(new InvoiceDiscount()
            .amountValue(null)
            .percentValue(1000))
        .delayInPaymentAllowed(null)
        .delayPenaltyPercent(null);
  }

  Invoice expectedDraft() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .comment(validInvoice().getComment())
        .ref(DRAFT_REF_PREFIX + validInvoice().getRef())
        .title("Facture sans produit")
        .customer(validInvoice().getCustomer())
        .status(DRAFT)
        .sendingDate(validInvoice().getSendingDate())
        .validityDate(validInvoice().getValidityDate())
        .delayInPaymentAllowed(DEFAULT_TO_PAY_DELAY_DAYS)
        .delayPenaltyPercent(DEFAULT_DELAY_PENALTY_PERCENT)
        .products(List.of(
            product4()
                .id(null)
                .totalVat(180)
                .totalPriceWithVat(1980),
            product5()
                .id(null)
                .totalVat(90)
                .totalPriceWithVat(990)))
        .totalPriceWithoutDiscount(3000)
        .totalPriceWithoutVat(1800 + 900) //with discount without vat
        .totalVat(180 + 90)
        .totalPriceWithVat(1980 + 990) //or 2700 + 270 of vat
        .globalDiscount(new InvoiceDiscount()
            .amountValue(300)
            .percentValue(1000))
        .paymentRegulations(List.of())
        .paymentType(CASH)
        .metadata(Map.of());
  }

  Invoice expectedConfirmed() {
    return new Invoice()
        .paymentUrl(null)
        .ref(confirmedInvoice().getRef())
        .title(confirmedInvoice().getTitle())
        .customer(confirmedInvoice().getCustomer())
        .status(CONFIRMED)
        .sendingDate(confirmedInvoice().getSendingDate())
        .products(List.of(product5().id(null)))
        .paymentRegulations(List.of(expectedDated1(), expectedDated2()))
        .paymentType(Invoice.PaymentTypeEnum.IN_INSTALMENT)
        .toPayAt(null)
        .delayInPaymentAllowed(confirmedInvoice().getDelayInPaymentAllowed())
        .delayPenaltyPercent(confirmedInvoice().getDelayPenaltyPercent())
        .totalPriceWithVat(1100)
        .totalVat(100)
        .totalPriceWithoutVat(1000)
        .totalPriceWithoutDiscount(1000)
        .globalDiscount(new InvoiceDiscount()
            .amountValue(0)
            .percentValue(0))
        .metadata(Map.of());
  }

  Invoice expectedInitializedDraft() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .products(List.of())
        .paymentRegulations(List.of())
        .paymentType(CASH)
        .totalVat(0)
        .totalPriceWithoutVat(0)
        .totalPriceWithoutDiscount(0)
        .totalPriceWithVat(0)
        .status(DRAFT)
        .delayInPaymentAllowed(DEFAULT_TO_PAY_DELAY_DAYS)
        .delayPenaltyPercent(DEFAULT_DELAY_PENALTY_PERCENT)
        .globalDiscount(new InvoiceDiscount()
            .percentValue(0)
            .amountValue(0))
        .metadata(Map.of());
  }

  Invoice expectedPaid() {
    return new Invoice()
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .ref(paidInvoice().getRef())
        .title(paidInvoice().getTitle())
        .customer(paidInvoice().getCustomer())
        .status(PAID)
        .sendingDate(paidInvoice().getSendingDate())
        .products(List.of(product5().id(null)))
        .paymentType(Invoice.PaymentTypeEnum.IN_INSTALMENT)
        .toPayAt(paidInvoice().getToPayAt())
        .delayInPaymentAllowed(paidInvoice().getDelayInPaymentAllowed())
        .delayPenaltyPercent(paidInvoice().getDelayPenaltyPercent())
        .totalPriceWithVat(1100)
        .totalVat(100)
        .totalPriceWithoutVat(1000)
        .totalPriceWithoutDiscount(1000)
        .metadata(Map.of())
        .globalDiscount(new InvoiceDiscount()
            .amountValue(0)
            .percentValue(0));
  }

  CrupdateInvoice validInvoice1() {
    return initializeDraft()
        .ref("BP003")
        .title("Facture sans produit")
        .comment("Nouveau commentaire")
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .sendingDate(LocalDate.now())
        .validityDate(LocalDate.now().plusDays(3L))
        .globalDiscount(new InvoiceDiscount()
            .amountValue(null)
            .percentValue(1000))
        .delayInPaymentAllowed(null)
        .status(PROPOSAL)
        .delayPenaltyPercent(null);
  }

  //TODO: create PaginationIT for pagination test and add filters.
  // In particular, check the date filters and the order filters (by created datetime desc)
  @Test
  @Order(1)
  void read_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actual1 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE1_ID);
    Invoice actual2 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE2_ID);
    List<Invoice> actualDraft = api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, 10, DRAFT);
    List<Invoice> actualNotFiltered = api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, 10, null);

    assertEquals(invoice1()
            .updatedAt(actual1.getUpdatedAt()),
        actual1);
    assertEquals(invoice2()
            .updatedAt(actual2.getUpdatedAt()),
        actual2);
    assertTrue(ignoreUpdatedAt(actualDraft).contains(invoice6()));
    assertTrue(ignoreUpdatedAt(actualNotFiltered).containsAll(
        List.of(
            actual1.updatedAt(null),
            actual2.updatedAt(null),
            invoice6().updatedAt(null))));
  }

  @Test
  @Order(2)
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
    assertThrowsApiException("{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "Invoice.not_existing_invoice_id is not found\"}",
        () -> api.getInvoiceById(JOE_DOE_ACCOUNT_ID, "not_existing_invoice_id"));
  }

  @Test
  @Order(3)
  void crupdate_invoice_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String firstInvoiceId = randomUUID().toString();
    CrupdateInvoice crupdateInvoiceWithNonExistentCustomer =
        initializeDraft().customer(customer1().id("non-existent-customer"));
    String uniqueRef = "unique_ref";
    List<CreateProduct> products = List.of(new CreateProduct()
            .description("Produit 1")
            .unitPrice(100)
            .quantity(1)
            .vatPercent(0),
        new CreateProduct()
            .description("Produit 2")
            .unitPrice(200)
            .quantity(1)
            .vatPercent(1000));
    Executable firstCrupdateExecutable =
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, firstInvoiceId,
            validInvoice().ref(uniqueRef));
    Executable secondCrupdateExecutable =
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, randomUUID().toString(),
            validInvoice().ref(uniqueRef));
    Executable thirdCrupdateExecutable =
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID,
            crupdateInvoiceWithNonExistentCustomer);
    Executable executable4 =
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, randomUUID().toString(),
            validInvoice().globalDiscount(new InvoiceDiscount()
                .amountValue(0)
                .percentValue(null)));
//    Executable executable5 =
//        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, randomUUID().toString(),
//            validInvoice().globalDiscount(new InvoiceDiscount()
//                .percentValue(null)
//                .amountValue(null)));
    Executable executable6 =
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, randomUUID().toString(),
            validInvoice().globalDiscount(new InvoiceDiscount()
                .percentValue(12000)
                .amountValue(null)));

    assertDoesNotThrow(firstCrupdateExecutable);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
            + "Invoice.reference=unique_ref is already used" + "\"}",
        secondCrupdateExecutable);
    assertThrowsApiException("{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "Customer." + crupdateInvoiceWithNonExistentCustomer.getCustomer().getId()
            + " is not found.\"}",
        thirdCrupdateExecutable);
    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\",\"message\":\""
            + "Only discount percent is supported for now" + "\"}", executable4);
//    assertThrowsApiException(
//        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
//            + "Discount percent is mandatory" + "\"}",
//        executable5);
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
            + "Discount percent 120.0% must be greater or equals to 0% and less or equals to 100%"
            + "\"}",
        executable6);
    assertThrowsApiException("{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"Multiple payments request more than one payment\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .paymentRegulations(List.of(new CreatePaymentRegulation()))));
    assertThrowsApiException("{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"Multiple payments amount 321 is not equals to total price with vat 320\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .products(products)
                .paymentRegulations(List.of(
                    new CreatePaymentRegulation()
                        .amount(261),
                    new CreatePaymentRegulation()
                        .amount(60)))));
    assertThrowsApiException("{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"Multiple payments percent 110.0% is not equals to 100%\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .products(products)
                .paymentRegulations(List.of(
                    new CreatePaymentRegulation()
                        .amount(null)
                        .percent(2000),
                    new CreatePaymentRegulation()
                        .amount(null)
                        .percent(9000)))));
    assertThrowsApiException("{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"Multiple payments percent 95.12% is not equals to 100%\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .products(products)
                .paymentRegulations(List.of(
                    new CreatePaymentRegulation()
                        .amount(null)
                        .percent(512),
                    new CreatePaymentRegulation()
                        .amount(null)
                        .percent(9000)))));
    assertThrowsApiException("{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"Multiple payments amount 20 is not equals to total price with vat 320\"}",
        () -> api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .products(products)
                .paymentRegulations(List.of(
                    new CreatePaymentRegulation()
                        .amount(10)
                        .percent(null),
                    new CreatePaymentRegulation()
                        .amount(10)
                        .percent(null)))));
  }

  @Test
  @Order(4)
  void crupdate_percent_mutiple_payments_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String id = String.valueOf(randomUUID());
    CrupdateInvoice crupdateInvoice = new CrupdateInvoice()
        .ref(id)
        .paymentType(IN_INSTALMENT)
        .customer(customer1()) //TODO: could not be null before creating a payment link
        .products(
            List.of(createProduct4())) //TODO: could not be null before creating a payment link
        .paymentRegulations(List.of(new CreatePaymentRegulation()
                .maturityDate(LocalDate.of(2023, 1, 1))
                .percent(2510)
                .comment("Acompte de 10%")
                .amount(null),
            new CreatePaymentRegulation()
                .maturityDate(LocalDate.of(2023, 1, 1))
                .percent(10000 - 2510)
                .comment("Reste 90%")
                .amount(null)));

    Invoice actualDraft = api.crupdateInvoice(
        JOE_DOE_ACCOUNT_ID, id, crupdateInvoice.status(DRAFT));
    actualDraft.setPaymentRegulations(ignoreIdsAndDatetime(actualDraft));
    Invoice actualProposal = api.crupdateInvoice(
        JOE_DOE_ACCOUNT_ID, id, crupdateInvoice
            .status(PROPOSAL)
            .paymentRegulations(List.of(new CreatePaymentRegulation()
                    .maturityDate(LocalDate.of(2023, 1, 1))
                    .percent(1025)
                    .comment("Acompte de 10%")
                    .amount(null),
                new CreatePaymentRegulation()
                    .maturityDate(LocalDate.of(2023, 1, 1))
                    .percent(10000 - 1025)
                    .comment("Reste 90%")
                    .amount(null))));
    actualProposal.setPaymentRegulations(ignoreIdsAndDatetime(actualProposal));
    Invoice actualConfirmed = api.crupdateInvoice(
        JOE_DOE_ACCOUNT_ID, id, crupdateInvoice.status(CONFIRMED));
    actualConfirmed.setPaymentRegulations(ignoreIdsAndDatetime(actualConfirmed));

    assertEquals(initPaymentReg(id), actualDraft.getPaymentRegulations());
    assertTrue(actualDraft.getPaymentRegulations().stream()
        .allMatch(
            paymentRegulation -> paymentRegulation.getPaymentRequest().getPaymentUrl() == null));
    assertEquals(updatedPaymentRegulations(id), actualProposal.getPaymentRegulations());
    assertTrue(actualProposal.getPaymentRegulations().stream()
        .allMatch(
            paymentRegulation -> paymentRegulation.getPaymentRequest().getPaymentUrl() == null));
    assertEquals(new Invoice()
        .id(actualConfirmed.getId())
        .ref(actualConfirmed.getRef())
        .paymentType(actualConfirmed.getPaymentType())
        .createdAt(actualConfirmed.getCreatedAt())
        .updatedAt(actualConfirmed.getUpdatedAt())
        .fileId(actualConfirmed.getFileId())
        .products(List.of(product4().id(null)))
        .totalVat(actualConfirmed.getTotalVat())
        .status(actualConfirmed.getStatus())
        .metadata(actualConfirmed.getMetadata())
        .toPayAt(actualConfirmed.getToPayAt())
        .sendingDate(actualConfirmed.getSendingDate())
        .totalPriceWithoutDiscount(2000)
        .totalPriceWithVat(actualConfirmed.getTotalPriceWithVat())
        .totalPriceWithoutVat(actualConfirmed.getTotalPriceWithoutVat())
        .customer(actualConfirmed.getCustomer())
        .delayPenaltyPercent(actualConfirmed.getDelayPenaltyPercent())
        .delayInPaymentAllowed(actualConfirmed.getDelayInPaymentAllowed())
        .paymentUrl(actualConfirmed.getPaymentUrl())
        .globalDiscount(new InvoiceDiscount()
            .amountValue(0)
            .percentValue(0))
        .paymentRegulations(confirmedPaymentRegulations(id)), actualConfirmed)
    ;
    assertTrue(actualConfirmed.getPaymentRegulations().stream()
        .allMatch(
            paymentRegulation -> paymentRegulation.getPaymentRequest().getPaymentUrl() != null));

  }

  // /!\ It seems that the localstack does not support the SES service using the default credentials
  // So note that SES service is mocked and do nothing for this test
  @Test
  @Order(1)
  void crupdate_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    int customizePenalty = 1960;

    Invoice actualDraft = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID,
            initializeDraft().ref(null))
        .delayPenaltyPercent(customizePenalty);
    Invoice actualUpdatedDraft = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID,
        validInvoice());
    actualUpdatedDraft.setProducts(ignoreIdsOf(actualUpdatedDraft.getProducts()));
    Invoice actualConfirmed =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE4_ID, confirmedInvoice());
    actualConfirmed.setProducts(ignoreIdsOf(actualConfirmed.getProducts()));
    actualConfirmed.setPaymentRegulations(ignoreIdsAndDatetime(actualConfirmed));
    Invoice actualPaid =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, actualConfirmed.getId(), paidInvoice());
    actualPaid.setProducts(ignoreIdsOf(actualPaid.getProducts()));
    actualPaid.setPaymentRegulations(ignoreIdsAndDatetime(actualPaid));

    assertEquals(expectedInitializedDraft().ref(null)
            .fileId(actualDraft.getFileId())
            .delayPenaltyPercent(customizePenalty)
            .createdAt(actualDraft.getCreatedAt())
            .updatedAt(actualDraft.getUpdatedAt()),
        actualDraft);
    assertNotNull(actualDraft.getFileId());
    assertNotEquals(DEFAULT_DELAY_PENALTY_PERCENT, actualDraft.getDelayPenaltyPercent());
    assertEquals(expectedDraft()
            .fileId(actualUpdatedDraft.getFileId())
            .createdAt(actualUpdatedDraft.getCreatedAt())
            .updatedAt(actualUpdatedDraft.getUpdatedAt()),
        actualUpdatedDraft
            //TODO: deprecated,remove when validity date is correctly set
            .toPayAt(null));
    assertNotNull(actualUpdatedDraft.getUpdatedAt());
    assertEquals(actualDraft.getFileId(), actualUpdatedDraft.getFileId());
    assertEquals(expectedConfirmed()
            .id(actualConfirmed.getId())
            .fileId(actualConfirmed.getFileId())
            .sendingDate(LocalDate.now())
            .updatedAt(actualConfirmed.getUpdatedAt()),
        actualConfirmed.createdAt(null));
    assertNotNull(actualConfirmed.getFileId());
    assertNotEquals(INVOICE4_ID, actualConfirmed.getId());
    assertNotNull(actualConfirmed.getUpdatedAt());
    assertEquals(expectedPaid()
        .fileId(actualPaid.getFileId())
        .id(actualPaid.getId())
        .paymentUrl(actualConfirmed.getPaymentUrl())
        .paymentRegulations(actualConfirmed.getPaymentRegulations())
        .sendingDate(actualConfirmed.getSendingDate())
        .toPayAt(actualConfirmed.getToPayAt())
        .createdAt(actualPaid.getCreatedAt())
        .updatedAt(actualPaid.getUpdatedAt()), actualPaid);
    assertNotNull(actualPaid.getFileId());
    assertNotNull(actualPaid.getUpdatedAt());
    assertEquals(actualConfirmed.getFileId(), actualPaid.getFileId());
    assertTrue(actualUpdatedDraft.getRef().contains(DRAFT_REF_PREFIX));
    assertFalse(actualConfirmed.getRef().contains(DRAFT_REF_PREFIX));
  }

  @Test
  @Order(3)
  void crupdate_with_null_discount_percent_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String id = String.valueOf(randomUUID());

    Invoice actual = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, id,
        new CrupdateInvoice()
            .status(DRAFT)
            .products(List.of(createProduct4()))
            .globalDiscount(new InvoiceDiscount()
                .percentValue(null)
                .amountValue(null)));
    actual.setProducts(ignoreIdsOf(actual.getProducts()));

    assertEquals(new Invoice()
            .id(id)
            .status(DRAFT)
            .products(List.of(product4().id(null)))
            .paymentType(CASH)
            .paymentRegulations(List.of())
            .globalDiscount(new InvoiceDiscount()
                .amountValue(0)
                .percentValue(0))
            .totalVat(actual.getTotalVat())
            .totalPriceWithoutDiscount(actual.getTotalPriceWithoutDiscount())
            .totalPriceWithVat(actual.getTotalPriceWithVat())
            .totalPriceWithoutVat(actual.getTotalPriceWithoutVat())
            .delayPenaltyPercent(actual.getDelayPenaltyPercent())
            .delayInPaymentAllowed(actual.getDelayInPaymentAllowed())
            .metadata(actual.getMetadata())
            .fileId(actual.getFileId())
            .updatedAt(actual.getUpdatedAt())
            .createdAt(actual.getCreatedAt()),
        actual);
  }

  @Test
  @Order(4)
  void second_update_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    Invoice invoice = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE1_ID);
    CrupdateInvoice crupdateInvoice = new CrupdateInvoice()
        .ref(invoice.getRef())
        .title(invoice.getTitle())
        .comment(invoice.getComment())
        .products(List.of(createProduct4(), createProduct5()))
        .status(invoice.getStatus())
        .sendingDate(invoice.getSendingDate())
        .validityDate(invoice.getValidityDate())
        .toPayAt(null);
    Invoice expected = TestUtils.invoice1()
        .products(List.of(product4().id(null), product5().id(null)))
        .paymentType(CASH)
        .toPayAt(null);

    Invoice actual = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, crupdateInvoice)
        .paymentUrl(expected.getPaymentUrl())
        .totalVat(expected.getTotalVat())
        .totalPriceWithVat(expected.getTotalPriceWithVat())
        .totalPriceWithoutVat(expected.getTotalPriceWithoutVat())
        .totalPriceWithoutDiscount(expected.getTotalPriceWithoutDiscount())
        .updatedAt(null)
        .customer(expected.getCustomer());

    assertEquals(INVOICE1_ID, actual.getId());
    assertEquals(expected.getRef(), actual.getRef());
    assertEquals(expected.getStatus(), actual.getStatus());
    assertNotEquals(expected, invoice);
    assertEquals(expected, actual);
  }

  @Test
  @Order(4)
  void crupdate_with_account_holder_not_subject_to_vat_ok() throws ApiException {
    reset(holderJpaRepository);
    when(holderJpaRepository.save(any()))
        .thenReturn(accountHolderEntity1()
            .toBuilder()
            .subjectToVat(false)
            .build());
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actual = api.crupdateInvoice(
        JOE_DOE_ACCOUNT_ID, String.valueOf(randomUUID()),
        initializeDraft()
            .ref(String.valueOf(randomUUID()))
            .products(List.of(createProduct4())));

    assertEquals(0, actual.getTotalVat());
    assertEquals(actual.getTotalPriceWithoutVat(), actual.getTotalPriceWithVat());
    assertTrue(actual.getTotalPriceWithVat() > 0);
  }

  //TODO: delete this test when validityDate is correctly set for draft invoice
  @Test
  @Order(4)
  void crupdate_with_null_validity_date_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    LocalDate today = LocalDate.now();

    Invoice actual = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, String.valueOf(randomUUID()),
        initializeDraft()
            .validityDate(null)
            .toPayAt(today));

    assertEquals(DRAFT, actual.getStatus());
    assertEquals(today, actual.getValidityDate());
    //TODO: deprecated, uncomment when validityDate is correctly set
    //assertNull(actual.getToPayAt());
  }

  @Test
  @Order(5)
  void update_invoice_product_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actualDraft = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, String.valueOf(randomUUID()),
        initializeDraft().ref(null)
            .products(List.of(createProduct4(),
                createProduct2())));
    Invoice actualDraftUpdated = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, actualDraft.getId(),
        initializeDraft().ref(null)
            .products(List.of(createProduct5())));
    actualDraftUpdated.setProducts(ignoreIdsOf(actualDraftUpdated.getProducts()));

    assertEquals(2, actualDraft.getProducts().size());
    assertEquals(1, actualDraftUpdated.getProducts().size());
    assertEquals(actualDraft.getId(), actualDraftUpdated.getId());
    assertTrue(actualDraftUpdated.getProducts().contains(product5().id(null)));
  }

  //TODO: uncomment when consumer handles overriding attributes
  //  @Test
  //  @Order(5)
  //  void update_invoice_customer_ok() throws ApiException {
  //    ApiClient joeDoeClient = anApiClient();
  //    PayingApi api = new PayingApi(joeDoeClient);
  //    Instant submittedAt = Instant.now();
  //    Map<String, String> submittedMetadata = Map.of("submittedAt", submittedAt.toString());
  //
  //    Invoice actualUpdated = api.crupdateInvoice(
  //        JOE_DOE_ACCOUNT_ID, invoice6().getId(),
  //        customerUpdatedInvoice().metadata(submittedMetadata));
  //    actualUpdated.setProducts(ignoreIdsOf(actualUpdated.getProducts()));
  //    Invoice actual = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, invoice6().getId());
  //    actual.setProducts(ignoreIdsOf(actual.getProducts()));
  //
  //    assertEquals(expectedCustomerUpdatedInvoice()
  //        .fileId(actual.getFileId())
  //        .metadata(submittedMetadata)
  //        .updatedAt(actualUpdated.getUpdatedAt())
  //        .createdAt(actualUpdated.getCreatedAt()), actualUpdated);
  //    assertEquals(expectedCustomerUpdatedInvoice()
  //        .fileId(actual.getFileId())
  //        .metadata(submittedMetadata)
  //        .updatedAt(actual.getUpdatedAt())
  //        .createdAt(actual.getCreatedAt()), actual);
  //    assertNotNull(actual.getFileId());
  //  }

  @Test
  @Order(6)
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
    assertTrue(actualProposal.getRef().contains(PROPOSAL_REF_PREFIX));
    assertTrue(fileUploadEvent.detail().contains(JOE_DOE_ACCOUNT_ID));
  }

  @Test
  @Order(1)
  void read_invoice_ordered_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Invoice> actual1 = api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, 5, null);
    List<Invoice> actual2 = api.getInvoices(JOE_DOE_ACCOUNT_ID, 2, 5, null);

    assertEquals(5, actual1.size());
    assertEquals(4, actual2.size());
    assertTrue(actual1.get(0).getCreatedAt().isAfter(actual1.get(1).getCreatedAt()));
    assertTrue(actual1.get(1).getCreatedAt().isAfter(actual1.get(2).getCreatedAt()));
    assertTrue(actual1.get(2).getCreatedAt().isAfter(actual1.get(3).getCreatedAt()));
    assertTrue(actual1.get(3).getCreatedAt().isAfter(actual1.get(4).getCreatedAt()));
    assertTrue(actual1.get(4).getCreatedAt().isAfter(actual2.get(0).getCreatedAt()));
    assertTrue(actual2.get(0).getCreatedAt().isAfter(actual2.get(1).getCreatedAt()));
  }

  @Test
  @Order(4)
  void invoice_fileId_from_proposal_to_confirmed_should_be_different() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    int customizePenalty = 1960;

    Invoice actualDraft = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID,
            initializeDraft().ref(null))
        .delayPenaltyPercent(customizePenalty);
    Invoice actualUpdatedProposalDraft = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID,
        validInvoice1());
    actualUpdatedProposalDraft.setProducts(ignoreIdsOf(actualUpdatedProposalDraft.getProducts()));
    Invoice actualConfirmed =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID, confirmedInvoice());
    Invoice actualPaid = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID, paidInvoice1());

    assertEquals(actualDraft.getFileId(), actualUpdatedProposalDraft.getFileId());
    assertNotEquals(actualUpdatedProposalDraft.getFileId(), actualConfirmed.getFileId());
    assertEquals(actualPaid.getFileId(), actualConfirmed.getFileId());
  }

  private List<Product> ignoreIdsOf(List<Product> actual) {
    return actual.stream()
        .peek(product -> product.setId(null))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<Invoice> ignoreUpdatedAt(List<Invoice> actual) {
    actual.forEach(invoice -> {
      invoice.setUpdatedAt(null);
    });
    return actual;
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