package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.TransactionIT.transaction2;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TransactionCategoryIT.ContextInitializer.class)
@AutoConfigureMockMvc
class TransactionCategoryIT {
  @MockBean
  private SentryConf sentryConf;

  @Value("${test.user.access.token}")
  private String bearerToken;

  TransactionCategory transactionCategory1() {
    return new TransactionCategory()
        .id("transaction_category1_id")
        .type("Recette TVA 20%")
        .userDefined(false)
        .vat(2000);
  }

  TransactionCategory transactionCategory2() {
    return new TransactionCategory()
        .id("transaction_category3_id")
        .type("Recette TVA 10%")
        .userDefined(false)
        .vat(1000);
  }

  TransactionCategory transactionCategory3() {
    return new TransactionCategory()
        .id("transaction_category3_id")
        .type("Recette TVA 10%")
        .userDefined(false)
        .vat(1000);
  }

  TransactionCategory transactionCategory4() {
    return new TransactionCategory()
        .id("transaction_category4_id")
        .type("Recette personnalisée TVA 1%")
        .userDefined(true)
        .vat(100);
  }

  TransactionCategory transactionCategory5() {
    return new TransactionCategory()
        .id("transaction_category5_id")
        .type("Recette personnalisée TVA 1,2%")
        .userDefined(true)
        .vat(120);
  }


  CreateTransactionCategory createTransactionCategory() {
    return new CreateTransactionCategory()
        .type("Recette TVA 1,5%")
        .vat(150);
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @Test
  void read_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actualAll = api.getTransactionCategories(JOE_DOE_ACCOUNT_ID, false,
        null);
    List<TransactionCategory> actualAllUnique = api.getTransactionCategories(JOE_DOE_ACCOUNT_ID,
        true,
        null);
    List<TransactionCategory> actualUnique =
        api.getTransactionCategories(JOE_DOE_ACCOUNT_ID, true, false);
    List<TransactionCategory> actualNotUnique =
        api.getTransactionCategories(JOE_DOE_ACCOUNT_ID, false, false);
    List<TransactionCategory> actualUserDefined =
        api.getTransactionCategories(JOE_DOE_ACCOUNT_ID, false, true);
    List<TransactionCategory> actualUniqueAndDefined =
        api.getTransactionCategories(JOE_DOE_ACCOUNT_ID, true, true);

    assertEquals(7, actualAll.size());
    assertEquals(5, actualAllUnique.size());
    assertEquals(2, actualUnique.size());
    assertEquals(3, actualNotUnique.size());
    assertEquals(4, actualUserDefined.size());
    assertEquals(3, actualUniqueAndDefined.size());
    assertTrue(actualUnique.contains(transactionCategory1()));
    assertTrue(actualUnique.contains(transactionCategory3()));
    assertTrue(actualNotUnique.containsAll(actualUnique));
    assertTrue(actualNotUnique.contains(transactionCategory2()));
    assertTrue(actualUserDefined.contains(transactionCategory4()));
    assertTrue(actualUserDefined.contains(transactionCategory5()));
  }

  @Test
  void create_transaction_categories_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PayingApi api = new PayingApi(joeDoeClient);

    List<TransactionCategory> actual = api.createTransactionCategories(JOE_DOE_ACCOUNT_ID,
        transaction2().getId(),
        List.of(createTransactionCategory()));

    assertEquals(1, actual.size());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
