package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.UsersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
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

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserIT.ContextInitializer.class)
@AutoConfigureMockMvc
class UserIT {
  private static final String BAD_TYPE = "BAD_TYPE";
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SwanComponent swanComponentMock;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

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

  @BeforeEach
  public void setUp() {
    TestUtils.setUpSwan(swanComponentMock);
  }

  @Test
  void unauthenticated_get_onboarding_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + UserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding"))
            .header("Access-Control-Request-Method", "GET")
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000")
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
  }

  @Test
  void user_read_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    UsersApi api = new UsersApi(user1Client);

    User actualUser = api.getUserById(TestUtils.USER1_ID);
    List<User> actualUsers = api.getUsers(1, 10, null, null, null);

    assertEquals(user1(), actualUser);
    assertEquals(1, actualUsers.size());
    assertTrue(actualUsers.contains(user1()));
  }

  @Test
  void user_read_by_bad_names_ko() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    UsersApi api = new UsersApi(user1Client);

    List<User> actualUsers =
        api.getUsers(1, 10, user1().getFirstName() + "123", user1().getLastName(),
            user1().getMobilePhoneNumber());


    assertEquals(0, actualUsers.size());
    assertFalse(actualUsers.contains(user1()));
  }

  @Test
  void user_read_by_criteria_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    UsersApi api = new UsersApi(user1Client);

    List<User> actualUsers = api.getUsers(1, 10, user1().getFirstName(), user1().getLastName(),
        user1().getMobilePhoneNumber());

    assertEquals(1, actualUsers.size());
    assertTrue(actualUsers.contains(user1()));
  }

  @Test
  void user_read_ignore_case_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    UsersApi api = new UsersApi(user1Client);

    List<User> actualUsers = api.getUsers(1, 10, user1().getFirstName().toUpperCase(),
        user1().getLastName().toUpperCase(),
        user1().getMobilePhoneNumber());

    assertEquals(1, actualUsers.size());
    assertTrue(actualUsers.contains(user1()));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
