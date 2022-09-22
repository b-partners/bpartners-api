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

import static app.bpartners.api.integration.conf.TestUtils.BAD_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = CustomerIT.ContextInitializer.class)
@AutoConfigureMockMvc
class CustomerIT {
  @MockBean
  UserSwanRepository swanRepositoryMock;
  @Value("${test.user.access.token}")
  private String bearerToken;
  @MockBean
  private SentryConf sentryConf;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpSwanRepository(swanRepositoryMock);
  }

  public static Customer customer1() {
    return new Customer()
        .id("customer1_id")
        .name("Luc Artisan")
        .email("luc@email.com")
        .phone("+33 12 34 56 78")
        .address("15 rue Porte d'Orange, Montmorency");
  }

  public static Customer customer2() {
    return new Customer()
        .id("customer2_id")
        .name("Jean Plombier")
        .email("jean@email.com")
        .phone("+33 12 34 56 78")
        .address("4 Avenue des Près, Montmorency");
  }

  CreateCustomer createCustomer1() {
    return new CreateCustomer()
        .name("Create customer 1")
        .phone("+33 12 34 56 78")
        .email("create@email.com")
        .address("New address");
  }

  @Test
  void read_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual = api.getCustomers(JOE_DOE_ACCOUNT_ID, null);
    List<Customer> actualFiltered = api.getCustomers(JOE_DOE_ACCOUNT_ID, "Jean");

    assertEquals(2, actual.size());
    assertEquals(1, actualFiltered.size());
    assertTrue(actual.contains(customer2()));
    assertTrue(actual.contains(customer1()));
    assertTrue(actual.contains(customer2()));
  }

  @Test
  void read_customers_ko() {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getCustomers(BAD_USER_ID, null));
  }

  @Test
  void create_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual =
        api.createCustomers(JOE_DOE_ACCOUNT_ID, List.of(createCustomer1()));

    List<Customer> actualList = api.getCustomers(JOE_DOE_ACCOUNT_ID, null);
    assertTrue(actualList.containsAll(actual));
  }

  @Test
  void create_customers_ko() {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createCustomers(BAD_USER_ID, List.of(createCustomer1())));
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
