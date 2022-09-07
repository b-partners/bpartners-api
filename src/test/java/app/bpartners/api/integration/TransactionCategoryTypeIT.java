package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategoryType;
import app.bpartners.api.endpoint.rest.model.TransactionCategoryType;
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
@ContextConfiguration(initializers = TransactionCategoryTypeIT.ContextInitializer.class)
@AutoConfigureMockMvc
class TransactionCategoryTypeIT {
  @MockBean
  private SentryConf sentryConf;

  @MockBean
  private SwanComponent swanComponentMock;

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
  }

  TransactionCategoryType transactionCategory1() {
    return new TransactionCategoryType()
        .id("category1_id")
        .label("label1");
  }

  CreateTransactionCategoryType validTransactionCategory() {
    return new CreateTransactionCategoryType()
        .label("valid_label");
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @Test
  void read_transactions_category_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    PayingApi api = new PayingApi(user1Client);

    List<TransactionCategoryType> actual = api.getTransactionCategories();

    assertTrue(actual.contains(transactionCategory1()));
  }

  @Test
  void create_transactions_category_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    PayingApi api = new PayingApi(user1Client);

    List<TransactionCategoryType> actual =
        api.createTransactionCategories(List.of(validTransactionCategory()));

    List<TransactionCategoryType> actualList = api.getTransactionCategories();
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
