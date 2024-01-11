package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunchConf;
import app.bpartners.api.integration.conf.S3MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE2_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInfoRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInitiationRep;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@AutoConfigureMockMvc
@Disabled("TODO(fail)")
class InvoiceRelaunchConfIT extends S3MockedThirdParties {
  private static final String RELAUNCH_CONF1_ID = "relaunchConf1_id";
  @MockBean private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean private FintecturePaymentInfoRepository paymentInfoRepositoryMock;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpPaymentInfoRepository(paymentInfoRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void read_conf_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    InvoiceRelaunchConf actual = api.getInvoiceRelaunchConf(JOE_DOE_ACCOUNT_ID, INVOICE2_ID);

    assertEquals(expectedRelaunchConf(), actual);
  }

  @Test
  void crupdate_conf_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    InvoiceRelaunchConf actual =
        api.configureInvoiceRelaunch(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, createInvoiceRelaunchConf());
    InvoiceRelaunchConf updated =
        api.configureInvoiceRelaunch(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, updateInvoiceRelaunchConf());

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
    return new InvoiceRelaunchConf().idInvoice(INVOICE1_ID).delay(5).rehearsalNumber(5);
  }

  InvoiceRelaunchConf updateInvoiceRelaunchConf() {
    return createInvoiceRelaunchConf().delay(10).rehearsalNumber(15);
  }
}
