package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.CustomerIT.customer1;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = InvoiceIT.ContextInitializer.class)
@AutoConfigureMockMvc
class InvoiceIT {
  @MockBean
  private SentryConf sentryConf;
  @Value("${test.user.access.token}")
  private String bearerToken;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, InvoiceIT.ContextInitializer.SERVER_PORT);
  }

  private static final String NEW_INVOICE_ID = "invoice_uuid";

  CrupdateInvoice validInvoice() {
    return new CrupdateInvoice()
        .ref("BP003")
        .title("Valid Invoice")
        .customer(customer1())
        .status(InvoiceStatus.CONFIRMED)
        .vat(20)
        .sendingDate(LocalDate.of(2022, 9, 10))
        .toPayAt(LocalDate.of(2022, 9, 11));
  }

  Invoice createdInvoice() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .ref(validInvoice().getRef())
        .customer(validInvoice().getCustomer())
        .status(InvoiceStatus.CONFIRMED)
        .vat(validInvoice().getVat())
        .sendingDate(validInvoice().getSendingDate())
        .products(List.of())
        .toPayAt(validInvoice().getToPayAt());
  }


  @Test
  void crupdate_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actual = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID, validInvoice());

    assertEquals(createdInvoice(), actual);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
