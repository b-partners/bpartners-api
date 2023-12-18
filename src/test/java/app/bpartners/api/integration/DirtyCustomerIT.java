package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.customer1;
import static app.bpartners.api.integration.conf.utils.TestUtils.customer2;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

import app.bpartners.api.endpoint.rest.api.CustomersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
class DirtyCustomerIT extends MockedThirdParties {

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
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

    List<Customer> actualNoFilter =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, null, null, 1, 20));
    List<Customer> actualFilteredByFirstAndLastName =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID, "Jean", "Plombier", null, null, null, null, null, null, 1, 20));
    List<Customer> actualFilteredByEmail =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID,
                null,
                null,
                "bpartners.artisans@gmail.com",
                null,
                null,
                null,
                null,
                null,
                1,
                20));
    List<Customer> actualFilteredByPhoneNumber =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID,
                null,
                null,
                null,
                "+33 12 34 56 78",
                null,
                null,
                null,
                null,
                1,
                20));
    List<Customer> actualFilteredByCity =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID, null, null, null, null, "Metz", null, null, null, 1, 20));
    List<Customer> actualFilteredByCountry =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID, null, null, null, null, null, "Allemagne", null, null, 1, 20));
    List<Customer> actualFilteredByFirstNameAndCity =
        ignoreUpdatedAndCreatedAt(
            api.getCustomers(
                JOE_DOE_ACCOUNT_ID,
                "Jean",
                null,
                null,
                null,
                "Montmorency",
                null,
                null,
                null,
                1,
                20));
    List<Customer> allFilteredResults = new ArrayList<>();
    Customer expectedCustomer1 = customer1().lastName("Nouvel artisan");

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
    assertTrue(actualNoFilter.contains(expectedCustomer1));
    assertTrue(actualNoFilter.contains(customer2()));
    assertTrue(actualFilteredByFirstAndLastName.contains(customer2()));
    assertTrue(actualFilteredByEmail.contains(expectedCustomer1));
    assertTrue(actualFilteredByPhoneNumber.contains(expectedCustomer1));
    assertTrue(actualFilteredByPhoneNumber.contains(customer2()));
    assertTrue(actualFilteredByCity.contains(expectedCustomer1));
    assertEquals("Jean Olivier", actualFilteredByCountry.get(0).getFirstName());
    assertTrue(actualNoFilter.containsAll(allFilteredResults));
  }

  @Test
  void update_then_read_customer_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CustomersApi api = new CustomersApi(joeDoeClient);
    var customersToUpdate =
        api.getCustomers(JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, null, null, 1, 20);
    var customerToUpdate = customersToUpdate.get(0);

    customerToUpdate.setLastName("Nouvel artisan");
    api.updateCustomers(JOE_DOE_ACCOUNT_ID, List.of(customerToUpdate));
    var updatedCustomer = api.getCustomerById(JOE_DOE_ACCOUNT_ID, customerToUpdate.getId());

    assertEquals("Nouvel artisan", updatedCustomer.getLastName());
  }

  public static List<Customer> ignoreUpdatedAndCreatedAt(List<Customer> customers) {
    return customers.stream()
        .peek(
            customer -> {
              customer.setCreatedAt(null);
              customer.setUpdatedAt(null);
            })
        .toList();
  }

  public static List<Customer> ignoreLatitudeAndLongitude(List<Customer> customers) {
    return customers.stream()
        .peek(
            customer -> {
              customer.getLocation().setLatitude(0.0);
              customer.getLocation().setLongitude(0.0);
            })
        .toList();
  }
}
