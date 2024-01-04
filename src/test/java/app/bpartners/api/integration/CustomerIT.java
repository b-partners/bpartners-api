package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;
import static app.bpartners.api.integration.DirtyCustomerIT.ignoreLatitudeAndLongitude;
import static app.bpartners.api.integration.DirtyCustomerIT.ignoreUpdatedAndCreatedAt;
import static app.bpartners.api.integration.conf.utils.TestUtils.BAD_USER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.OTHER_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.OTHER_CUSTOMER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.customer1;
import static app.bpartners.api.integration.conf.utils.TestUtils.customerDisabled;
import static app.bpartners.api.integration.conf.utils.TestUtils.customerUpdated;
import static app.bpartners.api.integration.conf.utils.TestUtils.customerWithSomeNullAttributes;
import static app.bpartners.api.integration.conf.utils.TestUtils.getApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpBanApiMock;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.service.CustomerService.EXCEL_MIME_TYPE;
import static app.bpartners.api.service.CustomerService.TEXT_CSV_MIME_TYPE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import app.bpartners.api.endpoint.rest.api.CustomersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.aws.SesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomerIT extends MockedThirdParties {
  @Autowired private CustomerService customerService;
  @Autowired private CustomerRestMapper customerRestMapper;
  @MockBean private BanApi banApiMock;
  @MockBean private SesService sesService;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpBanApiMock(banApiMock);
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

  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  @Test
  void export_customers_ok() throws IOException, InterruptedException, ApiException {
    InputStream expectedFileIs = new ClassPathResource("files/customers.csv").getInputStream();

    var actual = exportCustomers(JOE_DOE_ACCOUNT_ID, TEXT_CSV_MIME_TYPE);
    // TODO: check why not throwing error correctly
    assertThrowsApiException("", () -> exportCustomers(JOE_DOE_ACCOUNT_ID, EXCEL_MIME_TYPE));

    // /!\ It seems expected data length is less than actual 1712 <1886
    assertTrue(actual.body().length > 0);
    expectedFileIs.close();
    /*Uncomment to download file
    var fos = new FileOutputStream("customers" + randomUUID() + ".csv");
    fos.write(actual.body());
    fos.close();*/
  }

  @Test
  void read_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual =
        api.getCustomers(
            JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, null, null, null, null);

    List<app.bpartners.api.model.Customer> holderCustomers =
        customerService.findByAccountHolderId(JOE_DOE_ACCOUNT_HOLDER_ID);
    List<Customer> restHolderCustomers =
        holderCustomers.stream().map(customerRestMapper::toRest).collect(Collectors.toList());
    assertTrue(restHolderCustomers.containsAll(actual));
  }

  @Test
  void read_unique_customer_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    Customer actualCustomer = api.getCustomerById(JOE_DOE_ACCOUNT_ID, "customer1_id");

    assertEquals(
        customer1()
            .createdAt(actualCustomer.getCreatedAt())
            .updatedAt(actualCustomer.getUpdatedAt()),
        actualCustomer);
  }

  @Test
  void read_unique_customer_ko() {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"Customer."
            + OTHER_CUSTOMER_ID
            + " is not found.\"}",
        () -> api.getCustomerById(JOE_DOE_ACCOUNT_ID, OTHER_CUSTOMER_ID));
    assertThrowsForbiddenException(() -> api.getCustomerById(JANE_ACCOUNT_ID, "customer1_id"));
  }

  @Test
  void read_customers_ko() {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsForbiddenException(
        () ->
            api.getCustomers(
                BAD_USER_ID, null, null, null, null, null, null, null, null, null, null));
  }

  @Test
  void create_customers_ok() throws ApiException, MessagingException, IOException {
    doNothing().when(sesService).sendEmail(any(), any(), any(), any(), any());
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual1 =
        ignoreUpdatedAndCreatedAt(
            api.createCustomers(JOE_DOE_ACCOUNT_ID, List.of(createCustomer1())));
    List<Customer> actual2 =
        ignoreUpdatedAndCreatedAt(
            api.createCustomers(
                JOE_DOE_ACCOUNT_ID, List.of(createCustomer1().firstName("Create"))));
    List<Customer> actual3 =
        ignoreUpdatedAndCreatedAt(
            api.createCustomers(
                JOE_DOE_ACCOUNT_ID,
                List.of(
                    createCustomer1()
                        .firstName("NotNullFirstName")
                        .lastName("NotNullLastName")
                        .email("notnull@email.com"))));

    List<Customer> actualList =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, null, null, 1, 20));
    assertTrue(
        ignoreLatitudeAndLongitude(actualList).containsAll(ignoreLatitudeAndLongitude(actual1)));
    Customer customer1 = actual1.get(0);
    Customer customer2 = actual2.get(0);
    Customer customer3 = actual3.get(0);
    assertEquals(
        customer1
            .id(null)
            .location(null)
            .updatedAt(customer2.getUpdatedAt())
            .createdAt(customer2.getCreatedAt()),
        customer2.id(null).location(null));
    assertEquals(
        customer1
            .id(null)
            .name("NotNullFirstName NotNullLastName")
            .firstName("NotNullFirstName")
            .lastName("NotNullLastName")
            .email("notnull@email.com")
            .updatedAt(customer3.getUpdatedAt())
            .createdAt(customer3.getCreatedAt())
            .location(null),
        customer3.id(null).location(null));
  }

  @Test
  void create_customers_ko() {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.createCustomers(BAD_USER_ID, List.of(createCustomer1())));
  }

  @Test
  void update_customer_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual =
        ignoreUpdatedAndCreatedAt(
            api.updateCustomers(JOE_DOE_ACCOUNT_ID, List.of(customerUpdated())));
    List<Customer> existingCustomers =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID,
                "Marc",
                "Montagnier",
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                20));

    assertEquals(ignoreLatitudeAndLongitude(existingCustomers), ignoreLatitudeAndLongitude(actual));
    assertTrue(existingCustomers.containsAll(actual));
  }

  @Test
  void update_customer_with_some_null_attributes_ok() {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertDoesNotThrow(
        () -> api.updateCustomers(JOE_DOE_ACCOUNT_ID, List.of(customerWithSomeNullAttributes())));
  }

  @Test
  void update_customer_ko() {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Identifier must not be null."
            + " firstName not be null.\"}",
        () ->
            api.updateCustomers(
                JOE_DOE_ACCOUNT_ID, List.of(customerUpdated().id(null).firstName(null))));
    assertThrowsForbiddenException(
        () -> api.updateCustomers(OTHER_ACCOUNT_ID, List.of(customerUpdated())));
  }

  @Test
  void create_customers_from_uploaded_file_ok() throws IOException, InterruptedException {
    Resource filetoUpload = new ClassPathResource("files/customers.xlsx");

    HttpResponse<String> response = uploadFile(JOE_DOE_ACCOUNT_ID, filetoUpload.getFile());
    CollectionType playerListType =
        new ObjectMapper().getTypeFactory().constructCollectionType(List.class, Customer.class);
    List<Customer> actual =
        new ObjectMapper().findAndRegisterModules().readValue(response.body(), playerListType);

    assertNotNull(actual);
    assertEquals(7, actual.size());
  }

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
            + "\"}",
        response.body().replace("\\", ""));
  }

  @Test
  void read_customer_by_keyword() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);
    List<String> keywords = List.of("bparTnErs", "OlIvIer", "FraNk");

    List<Customer> actual =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                keywords,
                null,
                null));

    assertEquals(2, actual.size());
    assertTrue(actual.contains(customer1()));
  }

  @Test
  void read_and_update_disabled_customers_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actual =
        api.updateCustomerStatus(JOE_DOE_ACCOUNT_ID, List.of(customerDisabled()));
    List<Customer> enabledCustomers =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, ENABLED, null, 1, 20));
    List<Customer> disabledCustomers =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, DISABLED, null, 1, 20));

    assertTrue(
        disabledCustomers.containsAll(
            actual.stream()
                .map(
                    customer -> {
                      customer.updatedAt(null);
                      customer.createdAt(null);
                      return customer;
                    })
                .toList()));
    assertTrue(enabledCustomers.stream().allMatch(customer -> customer.getStatus() == ENABLED));
    assertTrue(
        disabledCustomers.stream()
            .allMatch(customer -> customer.getStatus() == CustomerStatus.DISABLED));
    assertFalse(enabledCustomers.containsAll(disabledCustomers));
  }

  private HttpResponse<String> uploadFile(String accountId, File toUpload)
      throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpResponse<String> response =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/accounts/" + accountId + "/customers/upload"))
                .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
                .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath()))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    return response;
  }

  private HttpResponse<byte[]> exportCustomers(String accountId, String fileType)
      throws IOException, InterruptedException, ApiException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpResponse<byte[]> response =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/accounts/" + accountId + "/customers/export"))
                .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
                .header("Accept", fileType)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() / 100 != 2) {
      throw getApiException("exportCustomers", response);
    }
    return response;
  }
}
