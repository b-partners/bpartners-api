package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PaymentApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.PaymentReq;
import app.bpartners.api.endpoint.rest.model.PaymentUrl;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PaymentIT.ContextInitializer.class)
@AutoConfigureMockMvc
class PaymentIT {
  @MockBean
  private SentryConf sentryConf;
  @Value("${test.user.access.token}")
  private String bearerToken;

  PaymentReq paymentReq1() {
    return new PaymentReq()
        .id("uuid")
        .amount(BigDecimal.valueOf(100))
        .label("Payment label")
        .reference("Payment reference")
        .payerName("Payer")
        .payerEmail("payer@email.com")
        .successUrl("https://dashboard-dev.bpartners.app")
        .failureUrl("https://dashboard-dev.bpartners.app/error");
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, PaymentIT.ContextInitializer.SERVER_PORT);
  }

  @Test
  void create_payment_req_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PaymentApi api = new PaymentApi(joeDoeClient);

    List<PaymentUrl> actual = api.createPaymentReq(List.of(paymentReq1()));

    PaymentUrl actualPaymentUrl = actual.get(0);
    assertTrue(
        actualPaymentUrl.getRedirectUrl().startsWith("https://connect-v2-sbx.fintecture.com"));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
