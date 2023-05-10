package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.LegalFile;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.service.PaymentScheduleService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.defaultLegalFile;
import static app.bpartners.api.integration.conf.TestUtils.legalFile1;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountConnectorSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = LegalFileIT.ContextInitializer.class)
@AutoConfigureMockMvc
class LegalFileIT {
  @MockBean
  private PaymentScheduleService paymentScheduleService;
  @MockBean
  private BuildingPermitConf buildingPermitConf;
  public static final String NOT_EXISTING_LEGAL_FILE = "not_existing_legal_file";
  @MockBean
  private UserSwanRepository swanRepositoryMock;
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
  private AccountConnectorRepository accountConnectorRepositoryMock;
  @MockBean
  private AccountHolderSwanRepository accountHolderMock;
  @MockBean
  private SwanComponent swanComponentMock;

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(swanRepositoryMock);
    setUpAccountConnectorSwanRepository(accountConnectorRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderMock);

  }

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
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

    assertEquals(5, actual.size());
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
    assertThrowsApiException("{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"LegalFile.legal_file1_id was already approved on 2022-01-01T00:00:00Z\"}",
        () -> api.approveLegalFile(JOE_DOE_ID, legalFile1().getId()));
    assertThrowsApiException("{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "LegalFile.not_existing_legal_file is not found\"}",
        () -> api.approveLegalFile(JOE_DOE_ID, NOT_EXISTING_LEGAL_FILE));
  }


  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
