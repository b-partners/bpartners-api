package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
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

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

  public static TransactionCategory transactionCategory1() {
    return new TransactionCategory()
        .id("transaction_category1_id")
        .type("Recette TVA 20%")
        .vat(2000)
        .userDefined(false);
  }

  static TransactionCategory transactionCategory6() {
    return new TransactionCategory()
        .id("transaction_category6_id")
        .type("Recette personnalisée TVA 1,2%")
        .vat(120)
        .userDefined(true);
  }

  public static Transaction transaction1() {
    return new Transaction()
        .id("bosci_0fe167566b234808a44aae415f057b6c")
        .label("Premier virement")
        .reference("JOE-001")
        .amount(BigDecimal.valueOf(500))
        .paymentDatetime(Instant.parse("2022-08-24T03:39:33.315Z"))
        .category(List.of(transactionCategory1()));
  }

  public static Transaction transaction2() {
    return new Transaction()
        .id("bosci_f224704f2555a42303e302ffb8e69eef")
        .label("Création de site vitrine")
        .reference("REF_001")
        .amount(BigDecimal.valueOf(500))
        .paymentDatetime(Instant.parse("2022-08-26T06:33:50.595Z"));
  }

  public static Transaction transaction3() {
    return new Transaction()
        .id("bosci_28cb4daf35d3ab24cb775dcdefc8fdab")
        .label("Test du virement")
        .reference("TEST-001")
        .amount(BigDecimal.valueOf(100))
        .paymentDatetime(Instant.parse("2022-08-24T04:57:02.606Z"))
        .category(List.of(transactionCategory6()));
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, TransactionIT.ContextInitializer.SERVER_PORT);
  }

  @Test
  void read_transactions_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actual = api.getTransactions(JOE_DOE_ACCOUNT_ID);

    assertEquals(3, actual.size());
    assertTrue(actual.contains(transaction1()));
    assertTrue(actual.contains(transaction2()));
    assertTrue(actual.contains(transaction3()));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
