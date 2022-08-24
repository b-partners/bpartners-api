package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.SecurityApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.repository.swan.schema.SwanUser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.BAD_REDIRECT_URL;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_URL;
import static app.bpartners.api.integration.conf.TestUtils.joeDoe;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserIT.ContextInitializer.class)
@AutoConfigureMockMvc
class UserIT {
  @MockBean
  private SentryConf sentryConf;
  @Value("${test.user.access.token}")
  private String bearerToken;


  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  User joeDoeUser() {
    SwanUser joeDoe = joeDoe();
    User user = new User();
    user.setId(TestUtils.JOE_DOE_ID);
    user.setSwanId(TestUtils.JOE_DOE_SWAN_USER_ID);
    user.setFirstName(joeDoe.firstName);
    user.setLastName(joeDoe.lastName);
    user.setBirthDate(joeDoe.birthDate);
    user.setIdVerified(joeDoe.idVerified);
    user.setIdentificationStatus(joeDoe.identificationStatus);
    user.setNationalityCCA3(joeDoe.nationalityCCA3);
    user.setMobilePhoneNumber(joeDoe.mobilePhoneNumber);
    user.setMonthlySubscription(5);
    user.setStatus(EnableStatus.ENABLED);
    return user;
  }

  @Test
  void unauthenticated_get_onboarding_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + UserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString("{\n"
                + "  \"successUrl\": \"" + REDIRECT_URL + "\",\n"
                + "  \"failureUrl\": \"failureUrl\"\n"
                + "}"))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
  }
  // /!\ The swan project access token provided by AWS SSM seems to not support two calls in a
  // same test
  /*@Test
  void unauthenticated_get_onboarding_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + UserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString("{\n"
                + "  \"successUrl\": \"" + BAD_REDIRECT_URL + "\",\n"
                + "  \"failureUrl\": \"string\"\n"
                + "}"))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
  }*/

  @Test
  void user_read_own_informations_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    SecurityApi api = new SecurityApi(joeDoeClient);

    User actualUser = api.whoami().getUser();

    assertEquals(joeDoeUser(), actualUser);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
