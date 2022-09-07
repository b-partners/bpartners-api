package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.CustomersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanRepository;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = CustomerIT.ContextInitializer.class)
@AutoConfigureMockMvc
class CustomerIT {
  @Value("${test.user.access.token}")
  private String bearerToken;
  @MockBean
  UserSwanRepository swanRepositoryMock;
  @MockBean
  private SentryConf sentryConf;

  @BeforeEach
  public void setUp() {
    setUpSwanRepository(swanRepositoryMock);
  }

  Customer customer1() {
    return new Customer()
        .id("customer1_id")
        .name("Customer 1")
        .email("customer1@email.com")
        .phone("+33 12 34 56 78")
        .address("Customer Address 1");
  }

  Customer customer2() {
    return new Customer()
        .id("customer2_id")
        .name("Customer 2")
        .email("customer2@email.com")
        .phone("+33 12 34 56 78")
        .address("Customer Address 2");
  }

  CreateCustomer createCustomer1() {
    return new CreateCustomer()
        .name("Create customer 1")
        .phone("+33 12 34 56 78")
        .email("create@email.com")
        .address("New address");
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @Test
  void read_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual = api.getCustomers(JOE_DOE_ACCOUNT_ID);

    assertTrue(actual.contains(customer1()));
    assertTrue(actual.contains(customer2()));
  }

  @Test
  void create_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual =
        api.createCustomers(JOE_DOE_ACCOUNT_ID, List.of(createCustomer1()));

    List<Customer> actualList = api.getCustomers(JOE_DOE_ACCOUNT_ID);
    assertTrue(actualList.containsAll(actual));
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
