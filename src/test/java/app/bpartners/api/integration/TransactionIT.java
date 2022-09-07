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
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
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

  Transaction transaction1() {
    return new Transaction()
        .id("bosci_0fe167566b234808a44aae415f057b6c")
        .label("Premier virement")
        .reference("JOE-001")
        .amount(BigDecimal.valueOf(500.0))
        .paymentDatetime(Instant.parse("2022-08-24T03:39:33.315Z"))
        .category(category1());
  }

  Transaction transaction2() {
    return new Transaction()
        .id("bosci_f224704f2555a42303e302ffb8e69eef")
        .label("Création de site vitrine")
        .reference("REF_001")
        .amount(BigDecimal.valueOf(500.0))
        .paymentDatetime(Instant.parse("2022-08-26T06:33:50.595Z"))
        .category(category2());
  }

  Transaction transactionWithNewCategory() {
    return new Transaction()
        .category(createTransactionCategory());
  }

  Transaction transactionWithBadOtherCategory() {
    return new Transaction()
        .category(badOtherCategory());
  }

  Transaction transactionWithBadCategory() {
    return new Transaction()
        .category(badCategory());
  }

  TransactionCategory category1() {
    return new TransactionCategory()
        .id("category1_id")
        .label("label1")
        .comment(null);
  }

  TransactionCategory category2() {
    return new TransactionCategory()
        .id("other_category_id")
        .label("Other")
        .comment("Commentaire de transaction");
  }

  TransactionCategory createTransactionCategory() {
    return new TransactionCategory()
        .id("other_category_id")
        .label("Other")
        .comment("Comment is mandatory");
  }

  TransactionCategory badOtherCategory() {
    return new TransactionCategory()
        .id("other_category_id")
        .label("Other")
        .comment(null);
  }

  TransactionCategory badCategory() {
    return new TransactionCategory()
        .id("category1_id")
        .label("label1")
        .comment("Comment is not allowed");
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
    assertTrue(actual.contains(transaction2()));
  }

  @Test
  void modify_transaction_category_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PayingApi api = new PayingApi(joeDoeClient);
    //TODO : Get transaction by ID before the change and compare with actual value

    Transaction actual = api.modifyTransaction(JOE_DOE_ACCOUNT_ID,
        transaction1().getId(),
        transactionWithNewCategory());

    assertEquals(transactionWithNewCategory().getCategory().getId(), actual.getCategory().getId());
    assertEquals(transactionWithNewCategory().getCategory().getComment(),
        actual.getCategory().getComment());
  }

  @Test
  void modify_transaction_category_ko() {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Comment is not allowed for category ["
            + badCategory().getLabel() + "]\"}",
        () -> api.modifyTransaction(JOE_DOE_ACCOUNT_ID,
            transaction1().getId(),
            transactionWithBadCategory()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"Comment is mandatory for the category type \\\"Other\\\"\"}",
        () -> api.modifyTransaction(JOE_DOE_ACCOUNT_ID,
            transaction1().getId(),
            transactionWithBadOtherCategory()));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
