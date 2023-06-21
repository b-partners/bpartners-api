package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.CreateAccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.createInvoiceRelaunchConf;
import static app.bpartners.api.integration.conf.TestUtils.invoiceRelaunchConf1;
import static app.bpartners.api.integration.conf.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserInvoiceRelaunchConfIT.ContextInitializer.class)
@AutoConfigureMockMvc
class UserInvoiceRelaunchConfIT {
  @MockBean
  private PaymentScheduleService paymentScheduleService;
  @MockBean
  private BuildingPermitConf buildingPermitConf;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private BridgeApi bridgeApi;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private AccountConnectorRepository accountConnectorRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private CognitoComponent cognitoComponentMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  private static AccountInvoiceRelaunchConf createdRelaunch() {
    CreateAccountInvoiceRelaunchConf toCreate = createInvoiceRelaunchConf();
    return new AccountInvoiceRelaunchConf()
        .unpaidRelaunch(toCreate.getUnpaidRelaunch())
        .draftRelaunch(toCreate.getDraftRelaunch());
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void read_invoice_relaunch_config_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    AccountInvoiceRelaunchConf actual = api.getAccountInvoiceRelaunchConf(JOE_DOE_ACCOUNT_ID);

    assertEquals(invoiceRelaunchConf1(), actual);
  }

  @Test
  void create_or_read_relaunch_config_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.getAccountInvoiceRelaunchConf("not" + JOE_DOE_ACCOUNT_ID)
    );
    assertThrowsForbiddenException(
        () -> api.configureAccountInvoiceRelaunch("not" + JOE_DOE_ACCOUNT_ID,
            createInvoiceRelaunchConf())
    );
  }

  @Test
  void create_invoice_relaunch_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    AccountInvoiceRelaunchConf expected = createdRelaunch();

    AccountInvoiceRelaunchConf actual =
        api.configureAccountInvoiceRelaunch(JOE_DOE_ACCOUNT_ID, createInvoiceRelaunchConf());
    expected.updatedAt(actual.getUpdatedAt())
        .id(actual.getId());

    assertEquals(expected, actual);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
