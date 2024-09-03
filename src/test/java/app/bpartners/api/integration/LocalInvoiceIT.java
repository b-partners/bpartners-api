package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.CrupdateInvoice.PaymentTypeEnum.IN_INSTALMENT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.MULTIPLE;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.UNKNOWN;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.discount_amount_not_supported_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.discount_percent_excedeed_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.first_ref_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.ignoreIdsAndDatetime;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.ignoreIdsOf;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.ignoreSeconds;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.initializeDraft;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.non_existent_customer_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.payment_reg_amount_higher_than_100_percent_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.payment_reg_amount_less_than_100_percent_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.payment_reg_more_than_one_payment_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.payment_reg_percent_higher_than_100_percent_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.payment_reg_percent_less_than_100_percent_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.unique_ref_violation_exec;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.validInvoice;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.NOT_JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.accountHolderEntity1;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.createProduct2;
import static app.bpartners.api.integration.conf.utils.TestUtils.createProduct4;
import static app.bpartners.api.integration.conf.utils.TestUtils.createProduct5;
import static app.bpartners.api.integration.conf.utils.TestUtils.customer1;
import static app.bpartners.api.integration.conf.utils.TestUtils.invoice1;
import static app.bpartners.api.integration.conf.utils.TestUtils.product5;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpEventBridge;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreatePaymentRegulation;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceReference;
import app.bpartners.api.endpoint.rest.model.InvoicesSummary;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.endpoint.rest.model.PaymentRegStatus;
import app.bpartners.api.endpoint.rest.model.PaymentRegulation;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.endpoint.rest.model.UpdateInvoiceArchivedStatus;
import app.bpartners.api.endpoint.rest.model.UpdatePaymentRegMethod;
import app.bpartners.api.integration.conf.S3MockedThirdParties;
import app.bpartners.api.integration.conf.utils.InvoiceTestUtils;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
class LocalInvoiceIT extends S3MockedThirdParties {
  @MockBean private BanApi banApi;
  @MockBean private AccountHolderJpaRepository holderJpaRepository;
  @MockBean private EventBridgeClient eventBridgeClientMock;
  @MockBean private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);

    when(holderJpaRepository.findAllByIdUser(JOE_DOE_ID))
        .thenReturn(List.of(accountHolderEntity1()));
  }

  public ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  @Test
  void read_invoices_summary_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    InvoicesSummary actual = api.getInvoicesSummary(JOE_DOE_ACCOUNT_ID);

    assertNotNull(actual);
    assertNotNull(actual.getLastUpdateDatetime());
    assertNotNull(actual.getPaid());
    assertNotNull(actual.getUnpaid());
    assertNotNull(actual.getProposal());
    assertNotNull(actual.getPaid().getAmount());
    assertNotNull(actual.getPaid().getCount());
    assertNotNull(actual.getUnpaid().getAmount());
    assertNotNull(actual.getUnpaid().getCount());
    assertNotNull(actual.getProposal().getAmount());
    assertNotNull(actual.getProposal().getCount());
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void read_invoice_by_filter_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Invoice> actualAll =
        api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, MAX_SIZE, null, null, null, null, null);
    List<Invoice> actualDraft =
        api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, MAX_SIZE, null, List.of(DRAFT), null, null, null);
    List<Invoice> actualProposal =
        api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, MAX_SIZE, null, List.of(PROPOSAL), null, null, null);
    List<Invoice> actualConfirmed =
        api.getInvoices(
            JOE_DOE_ACCOUNT_ID, 1, MAX_SIZE, null, List.of(CONFIRMED), null, null, null);
    List<Invoice> actualPaid =
        api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, MAX_SIZE, null, List.of(PAID), null, null, null);
    List<Invoice> actualConfirmedAndPaid =
        api.getInvoices(
            JOE_DOE_ACCOUNT_ID, 1, MAX_SIZE, null, List.of(CONFIRMED, PAID), null, null, null);
    String titleToFitler = "Facture";
    List<Invoice> actualByTitle =
        api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, MAX_SIZE, null, null, null, titleToFitler, null);

    assertTrue(
        actualAll.containsAll(
            Stream.of(actualDraft, actualProposal, actualConfirmed, actualPaid)
                .flatMap(List::stream)
                .collect(Collectors.toList())));
    assertTrue(actualDraft.stream().allMatch(invoice -> invoice.getStatus() == DRAFT));
    assertTrue(actualProposal.stream().allMatch(invoice -> invoice.getStatus() == PROPOSAL));
    assertTrue(actualConfirmed.stream().allMatch(invoice -> invoice.getStatus() == CONFIRMED));
    assertTrue(actualPaid.stream().allMatch(invoice -> invoice.getStatus() == PAID));
    assertTrue(
        actualConfirmedAndPaid.containsAll(
            Stream.of(actualConfirmed, actualPaid)
                .flatMap(List::stream)
                .collect(Collectors.toList())));
    assertTrue(
        actualByTitle.stream().allMatch(invoice -> invoice.getTitle().contains(titleToFitler)));
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void read_invoice_ordered_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Invoice> actual1 = api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, 5, null, null, null, null, null);
    List<Invoice> actual2 = api.getInvoices(JOE_DOE_ACCOUNT_ID, 2, 5, null, null, null, null, null);

    assertEquals(5, actual1.size());
    assertEquals(5, actual2.size());
    assertTrue(actual1.get(0).getCreatedAt().isAfter(actual1.get(1).getCreatedAt()));
    assertTrue(actual1.get(1).getCreatedAt().isAfter(actual1.get(2).getCreatedAt()));
    assertTrue(actual1.get(2).getCreatedAt().isAfter(actual1.get(3).getCreatedAt()));
    assertTrue(actual1.get(3).getCreatedAt().isAfter(actual1.get(4).getCreatedAt()));
    assertTrue(actual1.get(4).getCreatedAt().isAfter(actual2.get(0).getCreatedAt()));
    assertTrue(actual2.get(0).getCreatedAt().isAfter(actual2.get(1).getCreatedAt()));
  }

  @Test
  void duplicate_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String uuid = String.valueOf(randomUUID());
    String newRefValue = invoice1().getRef() + "-" + uuid;
    InvoiceReference newReference = new InvoiceReference().newReference(newRefValue + "-V2");
    Invoice invoice1 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE1_ID);
    CrupdateInvoice crupdateInvoice =
        crupdateFromExisting(invoice1, IN_INSTALMENT, paymentRegulations5050()).ref(newRefValue);
    Invoice initial = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, uuid, crupdateInvoice);

    Invoice actual = api.duplicateInvoice(JOE_DOE_ACCOUNT_ID, initial.getId(), newReference);
    actual.setPaymentRegulations(ignoreIdsAndDatetime(actual));
    actual.setProducts(ignoreIdsOf(Objects.requireNonNull(actual.getProducts())));

    Invoice initialAfter = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, initial.getId());
    assertNotEquals(initial.getFileId(), actual.getFileId());
    assertNotEquals(initial.getId(), actual.getFileId());
    assertEquals(initial, initialAfter);
    assertEquals(
        initial
            .paymentUrl(null)
            .ref("BROUILLON-" + newReference.getNewReference())
            .status(DRAFT)
            .updatedAt(actual.getUpdatedAt())
            .toPayAt(actual.getToPayAt())
            .createdAt(actual.getCreatedAt())
            .id(actual.getId()) // Ignoring it
            .fileId(actual.getFileId()) // Ignoring it
            .products(ignoreIdsOf(Objects.requireNonNull(initialAfter.getProducts())))
            .paymentRegulations(InvoiceTestUtils.ignoreIdsAndDatetimeAndUrl(initialAfter)),
        actual.paymentRegulations(ignoreSeconds(actual.getPaymentRegulations())));
  }

  @Test
  void update_invoice_product_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actualDraft =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            initializeDraft().ref(null).products(List.of(createProduct4(), createProduct2())));
    Invoice actualDraftUpdated =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            actualDraft.getId(),
            initializeDraft().ref(null).products(List.of(createProduct5())));
    actualDraftUpdated.setProducts(ignoreIdsOf(actualDraftUpdated.getProducts()));

    assertEquals(2, actualDraft.getProducts().size());
    assertEquals(1, actualDraftUpdated.getProducts().size());
    assertEquals(actualDraft.getId(), actualDraftUpdated.getId());
    assertTrue(actualDraftUpdated.getProducts().contains(product5().id(null)));
  }

  @Test
  void archive_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    Invoice initialInvoice =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            initializeDraft().ref(null).products(List.of(createProduct4(), createProduct2())));
    Invoice beforeChange = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, initialInvoice.getId());

    List<Invoice> actual =
        api.archiveInvoices(
            JOE_DOE_ACCOUNT_ID,
            List.of(new UpdateInvoiceArchivedStatus().id(INVOICE1_ID).archiveStatus(DISABLED)));

    assertNotEquals(beforeChange.getArchiveStatus(), actual.get(0).getArchiveStatus());
    assertEquals(DISABLED, actual.get(0).getArchiveStatus());
  }

  @Test
  void concurrently_update_invoice() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    Invoice initialInvoice =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            initializeDraft().ref(null).products(List.of(createProduct4(), createProduct2())));

    var callerNb = 10;
    var executor = newFixedThreadPool(10);

    var latch = new CountDownLatch(1);
    var futures = new ArrayList<Future<Invoice>>();
    var randomReference = randomUUID().toString();
    for (var callerIdx = 0; callerIdx < callerNb; callerIdx++) {
      futures.add(
          executor.submit(
              () -> crupdateInvoice(latch, api, initialInvoice.getId(), randomReference)));
    }
    latch.countDown();

    var retrieved =
        futures.stream()
            .map(TestUtils::getOptionalFutureResult)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toUnmodifiableList());
    assertEquals(callerNb, retrieved.size());
  }

  @Test
  void crupdate_invoice_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String firstInvoiceId = randomUUID().toString();
    CrupdateInvoice crupdateInvoiceWithNonExistentCustomer =
        initializeDraft().customer(customer1().id("non-existent-customer"));
    String uniqueRef = "unique_ref";
    List<CreateProduct> products =
        List.of(
            new CreateProduct().description("Produit 1").unitPrice(100).quantity(1).vatPercent(0),
            new CreateProduct()
                .description("Produit 2")
                .unitPrice(200)
                .quantity(1)
                .vatPercent(1000));
    assertDoesNotThrow(first_ref_exec(api, firstInvoiceId, uniqueRef));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
            + "La référence unique_ref est déjà utilisée"
            + "\"}",
        unique_ref_violation_exec(api, uniqueRef));
    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "Customer(id="
            + crupdateInvoiceWithNonExistentCustomer.getCustomer().getId()
            + ") not found\"}",
        non_existent_customer_exec(api, crupdateInvoiceWithNonExistentCustomer));
    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\",\"message\":\""
            + "Only discount percent is supported for now"
            + "\"}",
        discount_amount_not_supported_exec(api));
    //    assertThrowsApiException(
    //        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
    //            + "Discount percent is mandatory" + "\"}",
    //        executable5);
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
            + "Discount percent 120.0% must be greater or equals to 0% and less or equals to 100%"
            + "\"}",
        discount_percent_excedeed_exec(api));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"Multiple payments request more than one payment\"}",
        payment_reg_more_than_one_payment_exec(api));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"Multiple payments amount 321 is not equals to total price with vat 320\"}",
        payment_reg_amount_higher_than_100_percent_exec(api, products));

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"Multiple payments percent 110.0% is not equals to 100%\"}",
        payment_reg_percent_higher_than_100_percent_exec(api, products));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"Multiple payments percent 95.12% is not equals to 100%\"}",
        payment_reg_percent_less_than_100_percent_exec(api, products));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"Multiple payments amount 20 is not equals to total price with vat 320\"}",
        payment_reg_amount_less_than_100_percent_exec(api, products));
  }

  @Test
  void get_invoice_after_update_twice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    Invoice initialInvoice =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            initializeDraft()
                .ref(String.valueOf(randomUUID()))
                .customer(customer1())
                .products(List.of(createProduct4(), createProduct2())));
    String idInvoice = initialInvoice.getId();
    CrupdateInvoice crupdateInvoice =
        crupdateFromExisting(
            api.getInvoiceById(JOE_DOE_ACCOUNT_ID, idInvoice),
            IN_INSTALMENT,
            paymentRegulations1090());

    Invoice actual = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, idInvoice, crupdateInvoice);
    var paymentRegulations = actual.getPaymentRegulations();
    assertEquals(2970, paymentRegulations.get(0).getPaymentRequest().getAmount());
    assertEquals(330, paymentRegulations.get(1).getPaymentRequest().getAmount());

    Invoice actualRefetched = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, idInvoice);
    var paymentRegulationsRefecthed = actualRefetched.getPaymentRegulations();
    assertEquals(2970, paymentRegulationsRefecthed.get(0).getPaymentRequest().getAmount());
    assertEquals(330, paymentRegulationsRefecthed.get(1).getPaymentRequest().getAmount());

    Invoice actual2 =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            idInvoice,
            crupdateInvoice.paymentRegulations(
                List.of(
                    new CreatePaymentRegulation().percent(5000).maturityDate(LocalDate.now()),
                    new CreatePaymentRegulation()
                        .percent(5000)
                        .maturityDate(LocalDate.now().plusDays(10L)))));
    var paymentRegulations2 = actual2.getPaymentRegulations();
    assertEquals(1650, paymentRegulations2.get(0).getPaymentRequest().getAmount());
    assertEquals(1650, paymentRegulations2.get(1).getPaymentRequest().getAmount());

    Invoice actualRefetched2 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, idInvoice);
    var paymentRegulationsRefecthed2 = actualRefetched2.getPaymentRegulations();
    assertEquals(1650, paymentRegulationsRefecthed2.get(0).getPaymentRequest().getAmount());
    assertEquals(1650, paymentRegulationsRefecthed2.get(1).getPaymentRequest().getAmount());
  }

  private static List<CreatePaymentRegulation> paymentRegulations1090() {
    return List.of(
        new CreatePaymentRegulation().percent(9000).maturityDate(LocalDate.now()),
        new CreatePaymentRegulation().percent(1000).maturityDate(LocalDate.now().plusDays(10L)));
  }

  @Test
  void read_invoice_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.getInvoices(NOT_JOE_DOE_ACCOUNT_ID, 1, 10, null, null, null, null, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page must be >=1\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, -1, 10, null, null, null, null, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page size must be >=1\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, -10, null, null, null, null, null));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page size must be <" + MAX_SIZE + "\"}",
        () -> api.getInvoices(JOE_DOE_ACCOUNT_ID, 1, MAX_SIZE + 1, null, null, null, null, null));
    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "Invoice.not_existing_invoice_id is not found\"}",
        () -> api.getInvoiceById(JOE_DOE_ACCOUNT_ID, "not_existing_invoice_id"));
  }

  @Test
  @Disabled("TODO: fail after merging prod to preprod")
  void update_payment_regulation_method_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    Invoice initialInvoice =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            initializeDraft()
                .ref(String.valueOf(randomUUID()))
                .status(CONFIRMED)
                .customer(customer1())
                .products(Collections.singletonList(createProduct2()))
                .paymentType(IN_INSTALMENT)
                .paymentRegulations(paymentRegulations1090()));
    List<PaymentRegulation> paymentRegulations = initialInvoice.getPaymentRegulations();
    PaymentRegulation reg1 = paymentRegulations.get(0);
    PaymentRegulation reg2 = paymentRegulations.get(1);
    var payment1 = reg1.getPaymentRequest();
    var payment2 = reg2.getPaymentRequest();

    Invoice actual =
        api.updatePaymentRegMethod(
            JOE_DOE_ACCOUNT_ID,
            initialInvoice.getId(),
            payment1.getId(),
            new UpdatePaymentRegMethod().method(PaymentMethod.UNKNOWN));
    Invoice actual2 =
        api.updatePaymentRegMethod(
            JOE_DOE_ACCOUNT_ID,
            initialInvoice.getId(),
            payment2.getId(),
            new UpdatePaymentRegMethod().method(PaymentMethod.UNKNOWN));

    PaymentRegulation actualReg1 = actual.getPaymentRegulations().get(0);
    PaymentRegulation actualReg2 = actual.getPaymentRegulations().get(1);
    PaymentRegulation actualReg3 = actual2.getPaymentRegulations().get(0);
    PaymentRegulation actualReg4 = actual2.getPaymentRegulations().get(1);
    var actualPayment1 = actualReg1.getPaymentRequest();
    var actualPayment2 = actualReg2.getPaymentRequest();
    var actualPayment3 = actualReg3.getPaymentRequest();
    var actualPayment4 = actualReg4.getPaymentRequest();
    PaymentRegStatus reg1Status = reg1.getStatus();
    PaymentRegStatus reg2Status = reg2.getStatus();
    PaymentRegStatus actualReg1Status = actualReg1.getStatus();
    PaymentRegStatus actualReg2Status = actualReg2.getStatus();
    PaymentRegStatus actualReg3Status = actualReg3.getStatus();
    PaymentRegStatus actualReg4Status = actualReg4.getStatus();

    assertEquals(CONFIRMED, actual.getStatus());
    assertEquals(PaymentMethod.UNKNOWN, actual.getPaymentMethod());
    assertEquals(PAID, actual2.getStatus());
    assertEquals(MULTIPLE, actual2.getPaymentMethod());
    assertEquals(
        reg1Status
            .paymentStatus(PaymentStatus.PAID)
            .userUpdated(true)
            .paymentMethod(UNKNOWN)
            .updatedAt(actualReg1Status.getUpdatedAt()),
        actualReg1Status);
    assertEquals(
        reg2Status
            .paymentStatus(PaymentStatus.UNPAID)
            .userUpdated(null)
            .paymentMethod(null)
            .updatedAt(actualReg2Status.getUpdatedAt()),
        actualReg2Status);
    assertEquals(
        reg1Status
            .paymentStatus(PaymentStatus.PAID)
            .userUpdated(true)
            .paymentMethod(UNKNOWN)
            .updatedAt(actualReg3Status.getUpdatedAt()),
        actualReg3Status);
    assertEquals(
        reg2Status
            .paymentStatus(PaymentStatus.PAID)
            .userUpdated(true)
            .paymentMethod(UNKNOWN)
            .updatedAt(actualReg4Status.getUpdatedAt()),
        actualReg4Status);
    assertEquals(payment1.paymentStatus(PaymentStatus.PAID), actualPayment1);
    assertEquals(payment2.paymentStatus(PaymentStatus.UNPAID), actualPayment2);
    assertEquals(
        payment2
            .paymentStatus(PaymentStatus.PAID)
            .initiatedDatetime(actualPayment3.getInitiatedDatetime()),
        actualPayment3);
    assertEquals(
        payment1
            .paymentStatus(PaymentStatus.PAID)
            .initiatedDatetime(actualPayment4.getInitiatedDatetime()),
        actualPayment4);
  }

  @SneakyThrows
  private Invoice crupdateInvoice(
      CountDownLatch latch, PayingApi api, String idInvoice, String refInvoice) {
    latch.await();
    return api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, idInvoice, validInvoice().ref(refInvoice));
  }

  private static CrupdateInvoice crupdateFromExisting(
      Invoice invoice,
      CrupdateInvoice.PaymentTypeEnum paymentTypeEnum,
      List<CreatePaymentRegulation> paymentRegulations) {
    return new CrupdateInvoice()
        .ref(invoice.getRef())
        .title(invoice.getTitle())
        .comment(invoice.getComment())
        .products(List.of(createProduct4(), createProduct5()))
        .customer(invoice.getCustomer())
        .status(invoice.getStatus())
        .sendingDate(invoice.getSendingDate())
        .validityDate(invoice.getValidityDate())
        .paymentType(paymentTypeEnum)
        .paymentRegulations(paymentRegulations)
        .toPayAt(null);
  }

  private static List<CreatePaymentRegulation> paymentRegulations5050() {
    return List.of(
        new CreatePaymentRegulation().percent(5000).maturityDate(LocalDate.now()),
        new CreatePaymentRegulation().percent(5000).maturityDate(LocalDate.now().plusDays(10L)));
  }
}
