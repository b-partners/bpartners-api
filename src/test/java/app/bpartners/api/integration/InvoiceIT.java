package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.CrupdateInvoice.PaymentTypeEnum.IN_INSTALMENT;
import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.UNKNOWN;
import static app.bpartners.api.integration.AreaPictureIT.AREA_PICTURE_1_ID;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.*;
import static app.bpartners.api.integration.conf.utils.InvoiceTestUtils.ignoreStatusDatetime;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE4_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.accountHolderEntity1;
import static app.bpartners.api.integration.conf.utils.TestUtils.createProduct4;
import static app.bpartners.api.integration.conf.utils.TestUtils.customer1;
import static app.bpartners.api.integration.conf.utils.TestUtils.product4;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpEventBridge;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.model.Invoice.DEFAULT_DELAY_PENALTY_PERCENT;
import static java.util.UUID.randomUUID;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreatePaymentRegulation;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceDiscount;
import app.bpartners.api.integration.conf.S3MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
class InvoiceIT extends S3MockedThirdParties {
  @MockBean private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean private EventBridgeClient eventBridgeClientMock;
  @MockBean private AccountHolderJpaRepository holderJpaRepository;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);

    when(holderJpaRepository.findAllByIdUser(JOE_DOE_ID))
        .thenReturn(List.of(accountHolderEntity1()));
  }

  @Test
  void read_filtered_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Invoice> actualFiltered =
        api.getInvoices(
            JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, List.of("pOUr", "bp002"));

    assertEquals(2, actualFiltered.size());
    assertTrue(actualFiltered.stream().anyMatch(invoice -> invoice.getRef().equals("BP002")));
    assertTrue(
        actualFiltered.stream()
            .anyMatch(invoice -> invoice.getTitle().equals("Outils pour plomberie")));
  }

  @Test
  void crupdate_percent_multiple_payments_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String id = String.valueOf(randomUUID());
    CrupdateInvoice crupdateInvoice =
        new CrupdateInvoice()
            .title("Fabrication Jean")
            .ref(id)
            .paymentType(IN_INSTALMENT)
            .customer(customer1()) // TODO: could not be null before creating a payment link
            .products(
                List.of(createProduct4())) // TODO: could not be null before creating a payment link
            .paymentRegulations(
                List.of(
                    new CreatePaymentRegulation()
                        .maturityDate(LocalDate.of(2023, 1, 1))
                        .percent(2510)
                        .comment("Acompte de 10%")
                        .amount(null),
                    new CreatePaymentRegulation()
                        .maturityDate(LocalDate.of(2023, 1, 1))
                        .percent(10000 - 2510)
                        .comment("Reste 90%")
                        .amount(null)));

    Invoice actualDraft =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, id, crupdateInvoice.status(DRAFT));
    actualDraft.setPaymentRegulations(ignoreIdsAndDatetime(actualDraft));
    Invoice actualProposal =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            id,
            crupdateInvoice
                .status(PROPOSAL)
                .paymentRegulations(
                    List.of(
                        new CreatePaymentRegulation()
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
    Invoice actualConfirmed =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, id, crupdateInvoice.status(CONFIRMED));
    actualConfirmed.setPaymentRegulations(ignoreIdsAndDatetime(actualConfirmed));
    actualConfirmed.setProducts(ignoreIdsOf(actualConfirmed.getProducts()));
    actualConfirmed.setPaymentRegulations(ignoreStatusDatetime(actualConfirmed));

    assertEquals(initPaymentReg(id), ignoreStatusDatetime(actualDraft));
    assertTrue(
        actualDraft.getPaymentRegulations().stream()
            .allMatch(
                paymentRegulation ->
                    paymentRegulation.getPaymentRequest().getPaymentUrl() == null));
    assertEquals(updatedPaymentRegulations(id), ignoreStatusDatetime(actualProposal));
    assertTrue(
        actualProposal.getPaymentRegulations().stream()
            .allMatch(
                paymentRegulation ->
                    paymentRegulation.getPaymentRequest().getPaymentUrl() == null));
    assertEquals(
        expectedMultiplePayments(id, actualConfirmed).paymentRegulations(List.of()),
        actualConfirmed);
    assertTrue(
        actualConfirmed.getPaymentRegulations().stream()
            .allMatch(
                paymentRegulation ->
                    paymentRegulation.getPaymentRequest().getPaymentUrl() != null));
  }

  // note(no-ses):
  // /!\ It seems that the localstack does not support the SES service using the default
  // credentials. So note that SES service is mocked and do nothing for this test
  @Test
  @Order(4)
  // TODO: ordered tests are bad, use for example @DirtiesContext of SpringBootTest for resetting DB
  void crupdate_draft_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    int customizePenalty = 1960;

    Invoice actualDraft =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID, initializeDraft().ref(null));
    Invoice actualUpdatedDraft =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID, validInvoice().idAreaPicture(AREA_PICTURE_1_ID));
    actualUpdatedDraft.setProducts(ignoreIdsOf(actualUpdatedDraft.getProducts()));
    actualUpdatedDraft.setCustomer(ignoreCustomerDatetime(actualUpdatedDraft));

    assertEquals(
        expectedInitializedDraft()
            .ref(null)
            .fileId(actualDraft.getFileId())
            .delayPenaltyPercent(0)
            .createdAt(actualDraft.getCreatedAt())
            .updatedAt(actualDraft.getUpdatedAt()),
        actualDraft);
    assertNotNull(actualDraft.getFileId());
    assertEquals(DEFAULT_DELAY_PENALTY_PERCENT, actualDraft.getDelayPenaltyPercent());
    assertEquals(
        expectedDraft()
            .delayInPaymentAllowed(30)
            .fileId(actualUpdatedDraft.getFileId())
            .archiveStatus(ENABLED)
            .createdAt(actualUpdatedDraft.getCreatedAt())
            .updatedAt(actualUpdatedDraft.getUpdatedAt())
            .idAreaPicture("area_picture_1_id"),
        actualUpdatedDraft
            // TODO: deprecated,remove when validity date is correctly set
            .toPayAt(null));
    assertNotNull(actualUpdatedDraft.getUpdatedAt());
    assertEquals(actualDraft.getFileId(), actualUpdatedDraft.getFileId());
  }

  // note(no-ses)
  @Test
  @Order(4)
  void crupdate_confirmed_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actualConfirmed =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE4_ID, confirmedInvoice());
    actualConfirmed.setProducts(ignoreIdsOf(actualConfirmed.getProducts()));
    actualConfirmed.setPaymentRegulations(ignoreStatusDatetime(actualConfirmed));
    actualConfirmed.setCustomer(ignoreCustomerDatetime(actualConfirmed));
    Invoice actualPaid =
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, actualConfirmed.getId(), paidInvoice());
    actualPaid.setProducts(ignoreIdsOf(actualPaid.getProducts()));
    actualPaid.setPaymentRegulations(ignoreIdsAndDatetime(actualPaid));
    actualPaid.setPaymentRegulations(ignoreStatusDatetime(actualPaid));
    actualPaid.setCustomer(ignoreCustomerDatetime(actualPaid));

    assertEquals(
        expectedConfirmed()
            .id(actualConfirmed.getId())
            .fileId(actualConfirmed.getFileId())
            .archiveStatus(ENABLED)
            .paymentRegulations(List.of())
            .sendingDate(LocalDate.now())
            .updatedAt(actualConfirmed.getUpdatedAt()),
        actualConfirmed.createdAt(null));
    assertNotNull(actualConfirmed.getFileId());
    assertNotEquals(INVOICE4_ID, actualConfirmed.getId());
    assertNotNull(actualConfirmed.getUpdatedAt());
    assertEquals(
        expectedPaid()
            .fileId(actualPaid.getFileId())
            .id(actualPaid.getId())
            .paymentUrl(actualConfirmed.getPaymentUrl())
            .paymentRegulations(actualConfirmed.getPaymentRegulations())
            .sendingDate(actualConfirmed.getSendingDate())
            .toPayAt(actualConfirmed.getToPayAt())
            .createdAt(actualPaid.getCreatedAt())
            .updatedAt(actualPaid.getUpdatedAt()),
        actualPaid);
    assertNotNull(actualPaid.getFileId());
    assertNotNull(actualPaid.getUpdatedAt());
    assertEquals(actualConfirmed.getFileId(), actualPaid.getFileId());
    assertFalse(actualConfirmed.getRef().contains(DRAFT_REF_PREFIX));
  }

  @Test
  @Order(3)
  void crupdate_with_null_discount_percent_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String id = String.valueOf(randomUUID());

    Invoice actual =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            id,
            new CrupdateInvoice()
                .status(DRAFT)
                .products(List.of(createProduct4()))
                .globalDiscount(new InvoiceDiscount().percentValue(null).amountValue(null)));
    actual.setProducts(ignoreIdsOf(actual.getProducts()));

    assertEquals(
        new Invoice()
            .id(id)
            .status(DRAFT)
            .archiveStatus(ENABLED)
            .products(List.of(product4().id(null)))
            .paymentType(CASH)
            .paymentRegulations(List.of())
            .globalDiscount(new InvoiceDiscount().amountValue(0).percentValue(0))
            .totalVat(actual.getTotalVat())
            .totalPriceWithoutDiscount(actual.getTotalPriceWithoutDiscount())
            .totalPriceWithVat(actual.getTotalPriceWithVat())
            .totalPriceWithoutVat(actual.getTotalPriceWithoutVat())
            .delayPenaltyPercent(actual.getDelayPenaltyPercent())
            .delayInPaymentAllowed(actual.getDelayInPaymentAllowed())
            .metadata(actual.getMetadata())
            .fileId(actual.getFileId())
            .updatedAt(actual.getUpdatedAt())
            .createdAt(actual.getCreatedAt())
            .paymentMethod(UNKNOWN),
        actual);
  }

  // TODO:
  //  @Test
  //  @Order(4)
  //  void crupdate_with_account_holder_not_subject_to_vat_ok() throws ApiException {
  //    ApiClient joeDoeClient = anApiClient();
  //    PayingApi api = new PayingApi(joeDoeClient);
  //
  //    Invoice actual = api.crupdateInvoice(
  //        JOE_DOE_ACCOUNT_ID, String.valueOf(randomUUID()),
  //        initializeDraft()
  //            .ref(String.valueOf(randomUUID()))
  //            .products(List.of(createProduct4())));
  //
  //    assertEquals(0, actual.getTotalVat());
  //    assertEquals(actual.getTotalPriceWithoutVat(), actual.getTotalPriceWithVat());
  //    assertTrue(actual.getTotalPriceWithVat() > 0);
  //  }

  // TODO: delete this test when validityDate is correctly set for draft invoice

  // TODO: uncomment when consumer handles overriding attributes
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

  //  @Test
  //  @Order(6)
  //  void crupdate_triggers_event_ok() throws ApiException {
  //    ApiClient joeDoeClient = anApiClient();
  //    PayingApi api = new PayingApi(joeDoeClient);
  //    reset(eventBridgeClientMock);
  //    when(eventBridgeClientMock.putEvents((PutEventsRequest) any())).thenReturn(
  //        PutEventsResponse.builder().entries(
  //                PutEventsResultEntry.builder().eventId("eventId1").build())
  //            .build());
  //
  //    Invoice actualProposal =
  //        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE3_ID, proposalInvoice());
  //
  //    ArgumentCaptor<PutEventsRequest> captor = ArgumentCaptor.forClass(PutEventsRequest.class);
  //    verify(eventBridgeClientMock, times(1)).putEvents(captor.capture());
  //    PutEventsRequest actualRequest = captor.getValue();
  //    List<PutEventsRequestEntry> actualRequestEntries = actualRequest.entries();
  //    assertEquals(1, actualRequestEntries.size());
  //    PutEventsRequestEntry fileUploadEvent = actualRequestEntries.get(0);
  //    assertTrue(fileUploadEvent.detail().contains(actualProposal.getId()));
  //    assertTrue(actualProposal.getRef().contains(PROPOSAL_REF_PREFIX));
  //    assertTrue(fileUploadEvent.detail().contains(JOE_DOE_ACCOUNT_ID));
  //  }

  @Test
  void read_invoice_after_some_update() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String randomId = String.valueOf(randomUUID());

    Invoice invoice =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            randomId,
            new CrupdateInvoice()
                .ref(randomUUID().toString())
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .customer(customer1())
                .paymentRegulations(
                    List.of(
                        new CreatePaymentRegulation()
                            .maturityDate(LocalDate.of(2023, 1, 1))
                            .percent(1025)
                            .comment("Test de 10%")
                            .amount(null),
                        new CreatePaymentRegulation()
                            .maturityDate(LocalDate.of(2023, 1, 1))
                            .percent(10000 - 1025)
                            .comment("Test 90%")
                            .amount(null))));

    assertEquals(2, invoice.getPaymentRegulations().size());

    // First get
    Invoice persisted1 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, invoice.getId());
    // TODO: check why payment request createdDatetime is not the same
    invoice.setPaymentRegulations(ignoreIdsAndDatetime(invoice));
    persisted1.setPaymentRegulations(ignoreIdsAndDatetime(persisted1));
    assertEquals(
        persisted1.createdAt(null).updatedAt(null), invoice.createdAt(null).updatedAt(null));

    Invoice firstUpdate =
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            randomId,
            new CrupdateInvoice()
                .ref(randomUUID().toString())
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .customer(customer1())
                .paymentRegulations(
                    List.of(
                        new CreatePaymentRegulation()
                            .maturityDate(LocalDate.of(2023, 1, 1))
                            .percent(1025)
                            .comment("Tests de 10%")
                            .amount(null),
                        new CreatePaymentRegulation()
                            .maturityDate(LocalDate.of(2023, 1, 1))
                            .percent(10000 - 1025)
                            .comment("Test 90%")
                            .amount(null))));
    assertEquals(2, firstUpdate.getPaymentRegulations().size());

    Invoice peristed2 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, invoice.getId());
    assertEquals(2, peristed2.getPaymentRegulations().size());
  }
}
