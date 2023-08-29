package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunchConf;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.S3AbstractContextInitializer;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
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

import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE2_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInfoRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpS3Conf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = InvoiceRelaunchConfIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceRelaunchConfIT extends MockedThirdParties {
  private static final String RELAUNCH_CONF1_ID = "relaunchConf1_id";
  @MockBean
  private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean
  private FintecturePaymentInfoRepository paymentInfoRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        InvoiceRelaunchConfIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpPaymentInfoRepository(paymentInfoRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpS3Conf(s3Conf);
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

  static class ContextInitializer extends S3AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.findAvailableTcpPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
