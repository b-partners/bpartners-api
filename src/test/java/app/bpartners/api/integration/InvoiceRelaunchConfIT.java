package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE2_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = InvoiceRelaunchConfIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceRelaunchConfIT {
  private static final String RELAUNCH_CONF1_ID = "relaunchConf1_id";
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private SwanConf swanConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private AccountHolderSwanRepository accountHolderRepositoryMock;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @MockBean
  private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        InvoiceRelaunchConfIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  @Test
  @Order(1)
  void read_conf_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    InvoiceRelaunchConf actual = api.getInvoiceRelaunchConf(
        JOE_DOE_ACCOUNT_ID, INVOICE2_ID
    );

    assertEquals(expectedRelaunchConf(), actual);
  }

  @Test
  @Order(2)
  void crupdate_conf_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    InvoiceRelaunchConf actual = api.configureInvoiceRelaunch(
        JOE_DOE_ACCOUNT_ID, INVOICE1_ID,
        createInvoiceRelaunchConf()
    );
    InvoiceRelaunchConf updated = api.configureInvoiceRelaunch(
        JOE_DOE_ACCOUNT_ID, INVOICE1_ID, updateInvoiceRelaunchConf()
    );

    assertEquals(createInvoiceRelaunchConf().id(actual.getId()), actual);
    assertEquals(updateInvoiceRelaunchConf().id(actual.getId()), updated);
  }

  InvoiceRelaunchConf expectedRelaunchConf() {
    return new InvoiceRelaunchConf()
        .id(RELAUNCH_CONF1_ID)
        .idInvoice(INVOICE2_ID)
        .delay(1)
        .rehearsalNumber(1);
  }

  InvoiceRelaunchConf createInvoiceRelaunchConf() {
    return new InvoiceRelaunchConf()
        .idInvoice(INVOICE1_ID)
        .delay(5)
        .rehearsalNumber(5);
  }

  InvoiceRelaunchConf updateInvoiceRelaunchConf() {
    return createInvoiceRelaunchConf()
        .delay(10)
        .rehearsalNumber(15);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
