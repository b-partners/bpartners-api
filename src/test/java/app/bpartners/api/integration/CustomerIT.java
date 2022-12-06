package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.CustomersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.BAD_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.OTHER_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.OTHER_CUSTOMER_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.customer1;
import static app.bpartners.api.integration.conf.TestUtils.customerUpdated;
import static app.bpartners.api.integration.conf.TestUtils.customer2;
import static app.bpartners.api.integration.conf.TestUtils.customerWithSomeNullAttributes;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = CustomerIT.ContextInitializer.class)
@AutoConfigureMockMvc
class CustomerIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private SwanConf swanConf;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private AccountHolderSwanRepository accountHolderMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpSwanComponent(swanComponentMock);
    setUpAccountHolderSwanRep(accountHolderMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  CreateCustomer createCustomer1() {
    return new CreateCustomer()
        .name("Create customer 1")
        .phone("+33 12 34 56 78")
        .email("create@email.com")
        .website("https://customer.website.com")
        .address("New address")
        .zipCode(75001)
        .city("Paris")
        .country("France");
  }

  @Test
  void read_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual = api.getCustomers(JOE_DOE_ACCOUNT_ID, null);
    List<Customer> actualFiltered = api.getCustomers(JOE_DOE_ACCOUNT_ID, "Jean");

    assertEquals(3, actual.size());
    assertEquals(1, actualFiltered.size());
    assertTrue(actual.contains(customer1()));
    assertTrue(actual.contains(customer2()));
  }

  @Test
  void read_customers_ko() {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.getCustomers(BAD_USER_ID, null));
  }

  @Test
  void create_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual =
        api.createCustomers(JOE_DOE_ACCOUNT_ID, List.of(createCustomer1()));

    List<Customer> actualList = api.getCustomers(JOE_DOE_ACCOUNT_ID, null);
    assertTrue(actualList.containsAll(actual));
  }

  @Test
  void create_customers_ko() {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createCustomers(BAD_USER_ID, List.of(createCustomer1())));
  }

  @Test
  void update_customer_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual = api.updateCustomers(JOE_DOE_ACCOUNT_ID, List.of(customerUpdated()));
    List<Customer> customers = api.getCustomers(JOE_DOE_ACCOUNT_ID, "Marc Montagnier");

    assertEquals(customers.get(0), actual.get(0));
  }

  @Test
  void update_customer_with_some_null_attributes_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertDoesNotThrow(
        () -> api.updateCustomers(JOE_DOE_ACCOUNT_ID, List.of(customerWithSomeNullAttributes())));
  }

  @Test
  void update_customer_ko() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"Customer." + OTHER_CUSTOMER_ID
            + " is not found.\"}", () -> api.updateCustomers(JOE_DOE_ACCOUNT_ID,
            List.of(customerUpdated().id(OTHER_CUSTOMER_ID))));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Identifier must not be null."
            + " Name must not be null.\"}",
        () -> api.updateCustomers(JOE_DOE_ACCOUNT_ID,
            List.of(customerUpdated().id(null).name(null))));
    assertThrowsForbiddenException(
        () -> api.updateCustomers(OTHER_ACCOUNT_ID, List.of(customerUpdated())));
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
