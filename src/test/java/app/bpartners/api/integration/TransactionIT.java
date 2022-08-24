package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PaymentApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.math.BigDecimal;
import java.time.Instant;
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
@ContextConfiguration(initializers = TransactionIT.ContextInitializer.class)
@AutoConfigureMockMvc
class TransactionIT {
  @MockBean
  private SentryConf sentryConf;
  @Value("${test.user.access.token}")
  private String bearerToken;

  Transaction transaction1() {
    return new Transaction()
        .swanTransactionId("bosci_0fe167566b234808a44aae415f057b6c")
        .label("Premier virement")
        .reference("JOE-001")
        .currency("EUR")
        .amount(BigDecimal.valueOf(500.0))
        .paymentDatetime(Instant.parse("2022-08-24T03:39:33.315Z"))
        .category(category1());
  }

  TransactionCategory category1() {
    return new TransactionCategory()
        .id("category1_id")
        .label("label1");
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, TransactionIT.ContextInitializer.SERVER_PORT);
  }

  @Test
  void read_transactions_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PaymentApi api = new PaymentApi(joeDoeClient);

    List<Transaction> actual = api.getTransactions();

    assertTrue(actual.contains(transaction1()));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
