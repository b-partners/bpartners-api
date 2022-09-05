package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TransactionCategoryIT.ContextInitializer.class)
@AutoConfigureMockMvc
class TransactionCategoryIT {
  @MockBean
  private SentryConf sentryConf;

  @MockBean
  private SwanComponent swanComponentMock;

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
  }

  TransactionCategory transactionCategory1() {
    return new TransactionCategory()
        .id("category1_id")
        .label("label1");
  }

  CreateTransactionCategory validTransactionCategory() {
    return new CreateTransactionCategory()
        .label("valid_label");
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @Test
  void read_transactions_category_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    PayingApi api = new PayingApi(user1Client);

    List<TransactionCategory> actual = api.getTransactionCategories();

    assertTrue(actual.contains(transactionCategory1()));
  }

  @Test
  void create_transactions_category_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    PayingApi api = new PayingApi(user1Client);

    List<TransactionCategory> actual =
        api.createTransactionCategories(List.of(validTransactionCategory()));

    List<TransactionCategory> actualList = api.getTransactionCategories();
    assertTrue(actualList.containsAll(actual));
  }


  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
