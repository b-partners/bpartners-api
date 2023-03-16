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
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import static app.bpartners.api.integration.conf.TestUtils.BAD_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.OTHER_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.OTHER_CUSTOMER_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.customer1;
import static app.bpartners.api.integration.conf.TestUtils.customer2;
import static app.bpartners.api.integration.conf.TestUtils.customerUpdated;
import static app.bpartners.api.integration.conf.TestUtils.customerWithSomeNullAttributes;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = CustomerIT.ContextInitializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureMockMvc
class CustomerIT {
  @MockBean
  private BuildingPermitConf buildingPermitConf;
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
    return TestUtils.anApiClient(JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
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
        .firstName("Create")
        .lastName("customer 1")
        .phone("+33 12 34 56 78")
        .email("create@email.com")
        .website("https://customer.website.com")
        .address("New address")
        .zipCode(75001)
        .city("Paris")
        .country("France")
        .comment("Nouvelle rencontre");
  }

  @Order(1)
  @Test
  void read_and_filter_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actualNoFilter = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, 1, 20);
    List<Customer> actualFilteredByFirstAndLastName = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, "Jean", "Plombier", null, null, null, null, 1, 20);
    List<Customer> actualFilteredByEmail = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, "bpartners.artisans@gmail.com", null, null, null, 1, 20);
    List<Customer> actualFilteredByPhoneNumber = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, null, "+33 12 34 56 78", null, null, 1, 20);
    List<Customer> actualFilteredByCity = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, null, null, "Metz", null, 1, 20);
    List<Customer> actualFilteredByCountry = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, null, null, null, "Allemagne", 1, 20);
    List<Customer> actualFilteredByFirstNameAndCity = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, "Jean", null, null, null, "Montmorency", null, 1, 20);
    List<Customer> allFilteredResults = new ArrayList<>();
    allFilteredResults.addAll(actualFilteredByFirstAndLastName);
    allFilteredResults.addAll(actualFilteredByEmail);
    allFilteredResults.addAll(actualFilteredByPhoneNumber);
    allFilteredResults.addAll(actualFilteredByCity);
    allFilteredResults.addAll(actualFilteredByCountry);
    allFilteredResults.addAll(actualFilteredByFirstNameAndCity);

    assertEquals(4, actualNoFilter.size());
    assertEquals(1, actualFilteredByFirstAndLastName.size());
    assertEquals(1, actualFilteredByEmail.size());
    assertEquals(2, actualFilteredByPhoneNumber.size());
    assertEquals(1, actualFilteredByCity.size());
    assertEquals(1, actualFilteredByCountry.size());
    assertEquals(1, actualFilteredByFirstNameAndCity.size());
    assertTrue(actualNoFilter.contains(customer1()));
    assertTrue(actualNoFilter.contains(customer2()));
    assertTrue(actualFilteredByFirstAndLastName.contains(customer2()));
    assertTrue(actualFilteredByEmail.contains(customer1()));
    assertTrue(actualFilteredByPhoneNumber.contains(customer1()));
    assertTrue(actualFilteredByPhoneNumber.contains(customer2()));
    assertTrue(actualFilteredByCity.contains(customer1()));
    assertEquals("Jean Olivier", actualFilteredByCountry.get(0).getFirstName());
    assertTrue(actualNoFilter.containsAll(allFilteredResults));
  }

  @Order(1)
  @Test
  void read_customers_ko() {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.getCustomers(BAD_USER_ID, null, null, null, null, null, null, null, null));
  }

  @Order(2)
  @Test
  void create_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual1 =
        api.createCustomers(JOE_DOE_ACCOUNT_ID, List.of(createCustomer1()));
    List<Customer> actual2 =
        api.createCustomers(JOE_DOE_ACCOUNT_ID, List.of(createCustomer1().firstName("Create")));
    List<Customer> actual3 =
        api.createCustomers(JOE_DOE_ACCOUNT_ID,
            List.of(createCustomer1().firstName("NotNullFirstName").lastName("NotNullLastName")));

    List<Customer> actualList = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, 1, 20);
    assertTrue(actualList.containsAll(actual1));
    assertEquals(actual1.get(0).id(null), actual2.get(0).id(null));
    assertEquals(actual1.get(0)
        .id(null)
        .firstName("NotNullFirstName")
        .lastName("NotNullLastName"), actual3.get(0).id(null));
  }

  @Order(2)
  @Test
  void create_customers_ko() {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.createCustomers(BAD_USER_ID, List.of(createCustomer1())));
  }

  @Order(3)
  @Test
  void update_customer_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual = api.updateCustomers(JOE_DOE_ACCOUNT_ID, List.of(customerUpdated()));
    List<Customer> existingCustomers = api.getCustomers(JOE_DOE_ACCOUNT_ID,
        "Marc", "Montagnier", null, null, null, null, 1, 20);

    assertTrue(existingCustomers.containsAll(actual));
  }

  @Order(4)
  @Test
  void update_customer_with_some_null_attributes_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertDoesNotThrow(
        () -> api.updateCustomers(JOE_DOE_ACCOUNT_ID, List.of(customerWithSomeNullAttributes())));
  }

  @Order(4)
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
            + " firstName not be null.\"}", () -> api.updateCustomers(JOE_DOE_ACCOUNT_ID,
            List.of(customerUpdated().id(null).firstName(null))));
    assertThrowsForbiddenException(
        () -> api.updateCustomers(OTHER_ACCOUNT_ID, List.of(customerUpdated())));
  }

  @Order(5)
  @Test
  void create_customers_from_uploaded_file_ok() throws IOException, InterruptedException {
    Resource filetoUpload = new ClassPathResource("files/customers.xlsx");

    HttpResponse<String> response = uploadFile(JOE_DOE_ACCOUNT_ID, filetoUpload.getFile());
    CollectionType playerListType = new ObjectMapper().getTypeFactory()
        .constructCollectionType(List.class, Customer.class);
    List<Customer> actual = new ObjectMapper().findAndRegisterModules()
        .readValue(response.body(), playerListType);

    assertNotNull(actual);
    assertEquals(6, actual.size());
  }

  @Order(5)
  @Test
  void create_customers_from_uploaded_file_ko() throws IOException, InterruptedException {
    Resource file = new ClassPathResource("files/wrong-customers.xlsx");

    HttpResponse<String> response = uploadFile(JOE_DOE_ACCOUNT_ID, file.getFile());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    assertEquals(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"\"Nom\" instead of \"lastname\" at column 1. "
            + "\"Prénom(s)\" instead of \"firstname\" at column 2. "
            + "\"Email\" instead of \"mail\" at column 3. "
            + "\"Téléphone\" instead of \"phone\" at column 4. "
            + "\"Siteweb\" instead of \"site\" at column 5. "
            + "\"Adresse\" instead of \"address\" at column 6. "
            + "\"Code Postal\" instead of \"code\" at column 7. "
            + "\"Ville\" instead of \"city\" at column 8. "
            + "\"Pays\" instead of \"country\" at column 9. "
            + "\"Commentaires\" instead of \"comments\" at the last column."
            + "\"}", response.body().replace("\\", "")
    );
  }

  private HttpResponse<String> uploadFile(String accountId, File toUpload)
      throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + CustomerIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/accounts/" + accountId + "/customers/upload"))
            .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
            .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath())).build(),
        HttpResponse.BodyHandlers.ofString());

    return response;
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
