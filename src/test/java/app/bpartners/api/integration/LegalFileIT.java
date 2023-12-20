package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.conf.FacadeIT;
import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.LegalFile;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.connectors.account.AccountConnectorRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.defaultLegalFile;
import static app.bpartners.api.integration.conf.utils.TestUtils.legalFile1;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@AutoConfigureMockMvc
class LegalFileIT extends FacadeIT {
  @LocalServerPort
  protected int localPort;
  @MockBean
  protected PaymentScheduleService paymentScheduleService;
  @MockBean
  BuildingPermitConf buildingPermitConf;
  @MockBean
  SentryConf sentryConf;
  @MockBean
  SendinblueConf sendinblueConf;
  @MockBean
  S3Conf s3Conf;
  @MockBean
  CognitoComponent cognitoComponentMock;
  @MockBean
  FintectureConf fintectureConf;
  @MockBean
  ProjectTokenManager projectTokenManager;
  @MockBean
  AccountConnectorRepository accountConnectorRepositoryMock;
  @MockBean
  BridgeApi bridgeApi;
  @MockBean
  EventProducer eventProducer;

  public static final String NOT_EXISTING_LEGAL_FILE = "not_existing_legal_file";

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
  }

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @Test
  void read_legal_files_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getLegalFiles("NOT" + JOE_DOE_ID));
  }

  @Test
  void read_legal_files_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<LegalFile> actual = api.getLegalFiles(JOE_DOE_ID);

    assertEquals(7, actual.size());
    assertTrue(actual.contains(legalFile1().toBeConfirmed(true)));
  }

  @Test
  void approve_legal_file_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    LegalFile actual = api.approveLegalFile(JOE_DOE_ID, defaultLegalFile().getId());

    assertNotNull(actual.getApprovalDatetime());
  }

  @Test
  void approve_legal_file_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.approveLegalFile("NOT" + JOE_DOE_ID, legalFile1().getId()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"LegalFile.legal_file1_id was already approved on 2022-01-01T00:00:00Z\"}",
        () -> api.approveLegalFile(JOE_DOE_ID, legalFile1().getId()));
    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "LegalFile.not_existing_legal_file is not found\"}",
        () -> api.approveLegalFile(JOE_DOE_ID, NOT_EXISTING_LEGAL_FILE));
  }
}
