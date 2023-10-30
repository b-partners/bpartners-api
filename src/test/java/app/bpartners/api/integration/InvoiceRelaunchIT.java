package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Attachment;
import app.bpartners.api.endpoint.rest.model.CreateAttachment;
import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.endpoint.rest.model.EmailInfo;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunch;
import app.bpartners.api.endpoint.rest.model.RelaunchType;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.S3AbstractContextInitializer;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE3_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE7_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE8_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE_RELAUNCH1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE_RELAUNCH2_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.OTHER_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpEventBridge;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInfoRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpS3Conf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = InvoiceRelaunchIT.ContextInitializer.class)
@AutoConfigureMockMvc
class InvoiceRelaunchIT extends MockedThirdParties {
  @MockBean
  private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean
  private FintecturePaymentInfoRepository paymentInfoRepositoryMock;
  @MockBean
  private EventBridgeClient eventBridgeClientMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        InvoiceRelaunchIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpPaymentInfoRepository(paymentInfoRepositoryMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpS3Conf(s3Conf);
  }

  InvoiceRelaunch invoiceRelaunch1() {
    return new InvoiceRelaunch()
        .id(INVOICE_RELAUNCH1_ID)
        .type(RelaunchType.PROPOSAL)
        .accountId(JOE_DOE_ACCOUNT_ID)
        .isUserRelaunched(true)
        .emailInfo(new EmailInfo())
        .creationDatetime(Instant.parse("2022-01-01T01:00:00.00Z"))
        .attachments(List.of(attachment1(), attachment2()));
  }

  Attachment attachment1() {
    return new Attachment()
        .fileId("test.jpeg")
        .name("test file");
  }

  Attachment attachment2() {
    return new Attachment()
        .fileId("test.jpeg")
        .name("test file 2");
  }

  InvoiceRelaunch invoiceRelaunch2() {
    return new InvoiceRelaunch()
        .id(INVOICE_RELAUNCH2_ID)
        .type(RelaunchType.CONFIRMED)
        .accountId(JOE_DOE_ACCOUNT_ID)
        .isUserRelaunched(false)
        .emailInfo(new EmailInfo())
        .creationDatetime(Instant.parse("2022-01-01T01:00:00.00Z"))
        .attachments(List.of());
  }

  CreateInvoiceRelaunch creatableInvoiceRelaunch() throws IOException {
    return new CreateInvoiceRelaunch()
        .subject("relaunch_object")
        .message("<p>Email body</p>")
        .attachments(List.of(pngFileAttachment()));
  }

  CreateInvoiceRelaunch otherCreatableInvoiceRelaunch() {
    return new CreateInvoiceRelaunch()
        ._object("relaunch_object")
        .emailBody("<p>Email body</p>")
        .attachments(null);
  }

  InvoiceRelaunch expectedRelaunch() throws IOException {
    return new InvoiceRelaunch()
        .type(RelaunchType.CONFIRMED)
        .accountId(JOE_DOE_ACCOUNT_ID)
        .emailInfo(new EmailInfo()
            .emailObject("[NUMER] relaunch_object")
            .emailBody("<p>Email body</p>")
            .attachmentFileId("file1_id"))
        .isUserRelaunched(true)
        .attachments(List.of(expectedAttachment()));
  }

  CreateAttachment pngFileAttachment() throws IOException {
    Resource pngFile = new ClassPathResource("files/png-file.png");
    return new CreateAttachment()
        .name("attachment1")
        .content(pngFile.getInputStream().readAllBytes());
  }

  Attachment expectedAttachment() throws IOException {
    return new Attachment()
        .name(pngFileAttachment().getName())
        .fileId("file1_id");
  }

  @Test
  void relaunch_invoice_ok() throws ApiException, IOException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    InvoiceRelaunch actual =
        api.relaunchInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, creatableInvoiceRelaunch());
    actual.setAttachments(ignoreFileIdsOf(actual.getAttachments()));
    InvoiceRelaunch otherActual =
        api.relaunchInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, otherCreatableInvoiceRelaunch());
    assertEquals(
        expectedRelaunch()
            .id(actual.getId())
            .creationDatetime(actual.getCreationDatetime())
            .attachments(List.of(expectedAttachment().fileId(null)))
        , actual);
    assertEquals(
        expectedRelaunch()
            .id(otherActual.getId())
            .creationDatetime(otherActual.getCreationDatetime())
            .attachments(List.of()),
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

    assertTrue(actual.contains(invoiceRelaunch1()));
    assertFalse(confirmed.contains(invoiceRelaunch1()));
    assertTrue(proposals.contains(invoiceRelaunch1()));
    assertTrue(actual.contains(invoiceRelaunch2()));
    assertFalse(proposals.contains(invoiceRelaunch2()));
    assertTrue(confirmed.contains(invoiceRelaunch2()));
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
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
            + "Invoice.invoice8_id is already DISABLED."
            + "\"}",
        () -> api.relaunchInvoice(JOE_DOE_ACCOUNT_ID, INVOICE8_ID, creatableInvoiceRelaunch()));
  }

  private List<Attachment> ignoreFileIdsOf(List<Attachment> attachments) {
    if (attachments == null) {
      return null;
    }
    return attachments.stream()
        .peek(attachment -> attachment.setFileId(null))
        .toList();
  }

  static class ContextInitializer extends S3AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.findAvailableTcpPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
