package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.createInvoiceRelaunchConf;
import static app.bpartners.api.integration.conf.utils.TestUtils.invoiceRelaunchConf1;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.CreateAccountInvoiceRelaunchConf;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class UserInvoiceRelaunchConfIT extends MockedThirdParties {

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
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
        () -> api.getAccountInvoiceRelaunchConf("not" + JOE_DOE_ACCOUNT_ID));
    assertThrowsForbiddenException(
        () ->
            api.configureAccountInvoiceRelaunch(
                "not" + JOE_DOE_ACCOUNT_ID, createInvoiceRelaunchConf()));
  }

  @Test
  void create_invoice_relaunch_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    AccountInvoiceRelaunchConf expected = createdRelaunch();

    AccountInvoiceRelaunchConf actual =
        api.configureAccountInvoiceRelaunch(JOE_DOE_ACCOUNT_ID, createInvoiceRelaunchConf());
    expected.updatedAt(actual.getUpdatedAt()).id(actual.getId());

    assertEquals(expected, actual);
  }
}
