package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.CustomersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.customer1;
import static app.bpartners.api.integration.conf.TestUtils.customer2;
import static app.bpartners.api.integration.conf.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DbEnvContextInitializer.class)
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
class DirtyCustomerIT extends MockedThirdParties {

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, DbEnvContextInitializer.getHttpServerPort());
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void add_and_filter_customers() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);

    List<Customer> actualNoFilter = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null,
        null, 1, 20);
    List<Customer> actualFilteredByFirstAndLastName = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, "Jean", "Plombier", null, null, null, null,
        null, 1, 20);
    List<Customer> actualFilteredByEmail = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null,
        "bpartners.artisans@gmail.com", null, null, null,
        null, 1, 20);
    List<Customer> actualFilteredByPhoneNumber = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, null, "+33 12 34 56 78", null, null,
        null, 1, 20);
    List<Customer> actualFilteredByCity = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, null, null, "Metz", null,
        null, 1, 20);
    List<Customer> actualFilteredByCountry = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, null, null, null, null, null, "Allemagne",
        null, 1, 20);
    List<Customer> actualFilteredByFirstNameAndCity = api.getCustomers(
        JOE_DOE_ACCOUNT_ID, "Jean", null, null, null, "Montmorency", null,
        null, 1, 20);
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

  @Test
  void update_then_read_customer_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);
    var customersToUpdate = api.getCustomers(
        JOE_DOE_ACCOUNT_ID,
        null, null, null, null, null, null, null,
        1, 20);
    var customerToUpdate = customersToUpdate.get(0);

    String newLastName = randomUUID().toString();
    customerToUpdate.setLastName(newLastName);
    api.updateCustomers(JOE_DOE_ACCOUNT_ID, List.of(customerToUpdate));
    var updatedCustomer = api.getCustomerById(JOE_DOE_ACCOUNT_ID, customerToUpdate.getId());

    assertEquals(newLastName, updatedCustomer.getLastName());
  }
}
