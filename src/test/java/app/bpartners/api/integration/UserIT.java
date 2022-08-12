package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.UsersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserIT.ContextInitializer.class)
@AutoConfigureMockMvc
class UserIT {
  User user1() {
    User user = new User();
    user.setId(TestUtils.USER1_ID);
    user.setId(TestUtils.SWAN_USER1_ID);
    user.setFirstName("Mathieu");
    user.setLastName("Dupont");
    user.setBirthDate(LocalDate.of(2022, 8, 8));
    user.setIdVerified(true);
    user.setIdentificationStatus("ValidIdentity");
    user.setNationalityCCA3("FRA");
    user.setMobilePhoneNumber("+33123456789");
    user.setMonthlySubscription(5);
    user.setStatus(EnableStatus.ENABLED);
    return user;
  }

  User user2() {
    User user = new User();
    user.setId(TestUtils.USER2_ID);
    user.setId(TestUtils.SWAN_USER2_ID);
    user.setFirstName("Jean");
    user.setLastName("Dupont");
    user.setBirthDate(LocalDate.of(2022, 8, 8));
    user.setIdVerified(true);
    user.setIdentificationStatus("ValidIdentity");
    user.setNationalityCCA3("FRA");
    user.setMobilePhoneNumber("+33123456789");
    user.setMonthlySubscription(5);
    user.setStatus(EnableStatus.ENABLED);
    return user;
  }

  @MockBean
  private SwanConf swanConf;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SwanComponent swanComponentMock;

  private static final String INDIVIDUAL_ONBOARDING = "INDIVIDUAL";

  private static final String COMPANY_ONBOARDING = "COMPANY";

  private static final String BAD_TYPE = "BAD_TYPE";

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    TestUtils.setUpSwan(swanComponentMock);
  }
  /*
  @Test
  void unauthenticated_get_individual_onboarding_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + UserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding?type=" + INDIVIDUAL_ONBOARDING))
            .header("Access-Control-Request-Method", "GET")
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000")
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.MOVED_TEMPORARILY.value(), response.statusCode());
  }

  @Test
  void unauthenticated_get_company_onboarding_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + UserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding?type=" + COMPANY_ONBOARDING))
            .header("Access-Control-Request-Method", "GET")
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000")
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.MOVED_TEMPORARILY.value(), response.statusCode());
  }

  @Test
  void unauthenticated_get_onboards_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + UserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding?type=" + BAD_TYPE))
            .header("Access-Control-Request-Method", "GET")
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000")
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
  }*/


  @Test
  void user_read_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    UsersApi api = new UsersApi(user1Client);

    User actualUser = api.getUserById(TestUtils.USER1_ID);
    List<User> actualUsers = api.getUsers(1, 10);

    assertEquals(user1(), actualUser);
    assertEquals(3, actualUsers.size());
    assertTrue(actualUsers.contains(user1()));
    assertTrue(actualUsers.contains(user2()));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
