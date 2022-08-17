package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PreUsersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.model.PreUser;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.PRE_REGISTRATION1_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PreUserIT.ContextInitializer.class)
@AutoConfigureMockMvc
public class PreUserIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SwanComponent swanComponentMock;

  @MockBean
  private SwanConf swanConf;

  @BeforeEach
  public void setUp() {
    TestUtils.setUpSwan(swanComponentMock);
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  CreatePreUser validPreUser() {
    CreatePreUser createPreUser = new CreatePreUser();
    createPreUser.setEmail(TestUtils.VALID_EMAIL);
    createPreUser.setFirstName("john");
    createPreUser.setLastName("doe");
    createPreUser.setSociety("johnSociety");
    createPreUser.setPhoneNumber("+33 54 234 234");
    return createPreUser;
  }

  CreatePreUser badPreUser() {
    CreatePreUser createPreUser = new CreatePreUser();
    createPreUser.setEmail(TestUtils.INVALID_EMAIL);
    createPreUser.setFirstName("math");
    createPreUser.setLastName("doe");
    createPreUser.setSociety("johnSociety");
    createPreUser.setPhoneNumber("+33 54 234 234");
    return createPreUser;
  }

  PreUser preUser1() {
    PreUser preRegistration = new PreUser();
    preRegistration.setId(PRE_REGISTRATION1_ID);
    preRegistration.setEmail("mathieu@email.com");
    preRegistration.setFirstName("Mathieu");
    preRegistration.setLastName("Dupont");
    preRegistration.setSociety("Dupont SARL");
    preRegistration.setPhoneNumber("+33123456789");
    preRegistration.setEntranceDatetime(Instant.parse("2022-01-01T01:00:00.00Z"));
    return preRegistration;
  }

  @Test
  void unauthenticated_create_pre_users_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + PreUserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/preUsers"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(List.of(validPreUser()))))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
  }

  @Test
  void unauthenticated_read_pre_users_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + PreUserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/preUsers"))
            .header("Access-Control-Request-Method", "GET")
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000")
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.FORBIDDEN.value(), response.statusCode());
  }

  @Test
  void authenticated_read_pre_users_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TestUtils.USER1_TOKEN);
    PreUsersApi api = new PreUsersApi(apiClient);

    List<PreUser> actual = api.getPreUsers(1,10);

    assertTrue(actual.contains(preUser1()));
  }

  @Test
  void create_pre_users_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TestUtils.USER1_TOKEN);
    PreUsersApi api = new PreUsersApi(apiClient);

    List<PreUser> actual = api.createPreUsers(List.of(validPreUser()));

    List<PreUser> actualList = api.getPreUsers(1, 10);
    assertTrue(actualList.containsAll(actual));
  }

  @Test
  void create_pre_users_ko() {
    ApiClient apiClient = anApiClient(TestUtils.USER1_TOKEN);
    PreUsersApi api = new PreUsersApi(apiClient);

    TestUtils.assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invalid email\"}",
        () ->
            api.createPreUsers(List.of(badPreUser())));
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
